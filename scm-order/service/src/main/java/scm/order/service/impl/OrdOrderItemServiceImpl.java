package scm.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.order.domain.entity.OrdOrderItem;
import scm.order.mapper.OrdOrderItemMapper;
import scm.order.service.IOrdOrderItemService;

/**
 * <p>
 * 订单明细表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class OrdOrderItemServiceImpl extends ServiceImpl<OrdOrderItemMapper, OrdOrderItem> implements IOrdOrderItemService {

}
