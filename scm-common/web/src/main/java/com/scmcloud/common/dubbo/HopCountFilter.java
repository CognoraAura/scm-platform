package com.scmcloud.common.dubbo;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;

/**
 * Dubbo Filter enforcing the 3-hop rule for synchronous RPC chains.
 *
 * Prevents cascading timeout risk by limiting synchronous call depth.
 * Beyond hop 3, the call is rejected with an RpcException.
 *
 * The hop count is propagated via RpcContext attachment across service boundaries.
 */
@Slf4j
@Activate(group = CommonConstants.PROVIDER, order = -9999)
public class HopCountFilter implements Filter {

    private static final String HOP_COUNT_KEY = "rpc-hop-count";
    private static final int MAX_HOPS = 3;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String raw = RpcContext.getServiceContext().getAttachment(HOP_COUNT_KEY);
        int hops = (raw == null || raw.isBlank()) ? 1 : Integer.parseInt(raw) + 1;

        if (hops > MAX_HOPS) {
            String service = invoker.getInterface().getSimpleName();
            String method = invocation.getMethodName();
            throw new RpcException("RPC chain depth exceeded: " + hops
                    + " hops. Max allowed: " + MAX_HOPS
                    + ". Service: " + service + "." + method
                    + ". Use async (Kafka outbox) for downstream calls.");
        }

        RpcContext.getClientAttachment().setAttachment(HOP_COUNT_KEY, String.valueOf(hops));
        return invoker.invoke(invocation);
    }
}
