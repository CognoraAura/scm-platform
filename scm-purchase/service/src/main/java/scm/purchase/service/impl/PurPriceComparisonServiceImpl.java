package scm.purchase.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.purchase.domain.entity.PurPriceComparison;
import scm.purchase.mapper.PurPriceComparisonMapper;
import scm.purchase.service.IPurPriceComparisonService;

/**
 * <p>
 * 比价分析表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class PurPriceComparisonServiceImpl extends ServiceImpl<PurPriceComparisonMapper, PurPriceComparison>
        implements IPurPriceComparisonService {

}
