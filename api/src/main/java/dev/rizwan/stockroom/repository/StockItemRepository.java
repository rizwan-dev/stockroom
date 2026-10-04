package dev.rizwan.stockroom.repository;

import dev.rizwan.stockroom.domain.StockItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {

    @Query("select s from StockItem s join fetch s.product order by s.product.sku")
    List<StockItem> findAllWithProduct();
}
