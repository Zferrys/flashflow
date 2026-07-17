package com.flashflow.promotion.dao;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashflow.promotion.entity.ProductSku;
import org.apache.ibatis.annotations.Select;
import java.util.List;
public interface ProductSkuMapper extends BaseMapper<ProductSku> {
    @Select("SELECT * FROM product_sku WHERE spu_id = #{spuId} ORDER BY price")
    List<ProductSku> selectBySpuId(Long spuId);
}
