package com.scmcloud.common.data.rw.sql;

import com.scmcloud.common.data.rw.routing.ReadWriteRoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * MyBatis SQL 璺敱鎷︽埅锟?
 * <p>
 * 鍙傝€冿細
 * - 缇庡洟 Zebra ZebraInterceptor
 * - Apache ShardingSphere SQLRouteExecutor
 * <p>
 * 锟絊QL 鎵ц鍓嶈В锟絊QL 绫诲瀷锟紿int锛岃缃矾鐢变笂涓嬫枃
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class SqlRoutingInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 濡傛灉宸茬粡鏈夋樉寮忚矾鐢辫缃紝涓嶅啀澶勭悊
        if (ReadWriteRoutingContext.current() != ReadWriteRoutingContext.RoutingType.AUTO) {
            return invocation.proceed();
        }

        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];

        // 鑾峰彇 SQL
        BoundSql boundSql = ms.getBoundSql(parameter);
        String sql = boundSql.getSql();

        // 1. 瑙ｆ瀽 Hint
        SqlTypeParser.RoutingHint hint = SqlTypeParser.parseHint(sql);
        if (hint.type() != SqlTypeParser.RoutingHint.HintType.NONE) {
            return executeWithHint(invocation, hint);
        }

        // 2. 鏍规嵁 MyBatis SqlCommandType 鍒ゆ柇
        SqlCommandType commandType = ms.getSqlCommandType();
        if (commandType == SqlCommandType.SELECT) {
            // 杩涗竴姝ユ鏌ユ槸鍚︽湁 FOR UPDATE
            SqlTypeParser.SqlType sqlType = SqlTypeParser.parse(sql);
            if (sqlType == SqlTypeParser.SqlType.WRITE) {
                // SELECT ... FOR UPDATE锛岃蛋涓诲簱
                log.debug("[SQL-Routing] Detected SELECT FOR UPDATE, routing to MASTER");
                return executeWithMaster(invocation);
            }

            // 鏅拷SELECT锛岃蛋浠庡簱
            log.debug("[SQL-Routing] Detected SELECT, routing to SLAVE");
            return executeWithSlave(invocation);
        }

        // INSERT/UPDATE/DELETE锛岃蛋涓诲簱骞舵爣璁板啓鎿嶄綔
        log.debug("[SQL-Routing] Detected {} operation, routing to MASTER", commandType);
        return executeWithMasterAndMarkWrite(invocation);
    }

    private Object executeWithHint(Invocation invocation, SqlTypeParser.RoutingHint hint)
            throws Throwable {
        switch (hint.type()) {
            case MASTER -> {
                log.debug("[SQL-Routing] Hint: MASTER");
                return executeWithMaster(invocation);
            }
            case SLAVE -> {
                if (hint.slaveName() != null) {
                    log.debug("[SQL-Routing] Hint: SLAVE({})", hint.slaveName());
                    ReadWriteRoutingContext.specifySlave(hint.slaveName());
                } else {
                    log.debug("[SQL-Routing] Hint: SLAVE");
                }
                return executeWithSlave(invocation);
            }
            default -> {
                return invocation.proceed();
            }
        }
    }

    private Object executeWithMaster(Invocation invocation) throws Throwable {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
        try {
            return invocation.proceed();
        } finally {
            ReadWriteRoutingContext.pop();
        }
    }

    private Object executeWithMasterAndMarkWrite(Invocation invocation) throws Throwable {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
        try {
            Object result = invocation.proceed();
            // 鍐欐搷浣滄垚鍔熷悗鏍囪
            ReadWriteRoutingContext.markWrite();
            return result;
        } finally {
            ReadWriteRoutingContext.pop();
        }
    }

    private Object executeWithSlave(Invocation invocation) throws Throwable {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);
        try {
            return invocation.proceed();
        } finally {
            ReadWriteRoutingContext.pop();
            ReadWriteRoutingContext.specifySlave(null); // 娓呴櫎鎸囧畾鐨勪粠锟?
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
}
