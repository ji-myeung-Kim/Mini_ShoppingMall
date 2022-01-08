package jpabook.jpashop.repository.order.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;

    public List<OrderQueryDto> findOrderQueryDtos() {
        List<OrderQueryDto> result = findOrders(); // query1번 -> 2번(N개) ----> N+1문제

        result.forEach(o -> {
            List<OrderItemQueryDto> orderItems = findOrderItems(o.getOrderId()); //Query 2번
            o.setOrderItems(orderItems);
        });
        return result;
    }

    private List<OrderItemQueryDto> findOrderItems(Long orderId) {
        return em.createQuery("" +
                "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                " from OrderItem oi" +
                " join oi.item i" +
                " where oi.order.id = :orderId", OrderItemQueryDto.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    private List<OrderQueryDto> findOrders() {
        return em.createQuery(

                "select new jpabook.jpashop.repository.order.query.OrderQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                        " from Order o" +
                        " join o.member m" +
                        " join o.delivery d", OrderQueryDto.class
        ).getResultList();
    }

    public List<OrderQueryDto> findAllByDtoOptimization() {

        /**
         * select
         *         order0_.order_id as col_0_0_,
         *         member1_.name as col_1_0_,
         *         order0_.order_date as col_2_0_,
         *         order0_.status as col_3_0_,
         *         delivery2_.city as col_4_0_,
         *         delivery2_.street as col_4_1_,
         *         delivery2_.zipcode as col_4_2_
         *     from
         *         orders order0_
         *     inner join
         *         member member1_
         *             on order0_.member_id=member1_.member_id
         *     inner join
         *         delivery delivery2_
         *             on order0_.delivery_id=delivery2_.delivery_id
         */
        List<OrderQueryDto> result = findOrders();

        List<Long> orderIds = toOrderIds(result);

        /**
         * select
         *         orderitem0_.order_id as col_0_0_,
         *         item1_.name as col_1_0_,
         *         orderitem0_.order_price as col_2_0_,
         *         orderitem0_.count as col_3_0_
         *     from
         *         order_item orderitem0_
         *     inner join
         *         item item1_
         *             on orderitem0_.item_id=item1_.item_id
         *     where
         *         orderitem0_.order_id in (
         *             ? , ?
         *         )
         */
        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(orderIds);

        result.forEach(o -> o.setOrderItems(orderItemMap.get(o.getOrderId())));
        return result;
    }

    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {
        List<OrderItemQueryDto> orderItems = em.createQuery("" +
                        "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                        " from OrderItem oi" +
                        " join oi.item i" +
                        " where oi.order.id in :orderIds", OrderItemQueryDto.class)
                .setParameter("orderIds", orderIds)
                .getResultList();

        /**
         * Long에 OrderItemQueryDto.getOrderId()가 키값으로 들어가고 List<OrderItemQueryDto>가 벨류값으로 list형태로 출력
         */
        Map<Long, List<OrderItemQueryDto>> orderItemMap = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItemQueryDto -> OrderItemQueryDto.getOrderId()));
        return orderItemMap;
    }

    private List<Long> toOrderIds(List<OrderQueryDto> result) {
        List<Long> orderIds = result.stream()
                .map(o -> o.getOrderId())
                .collect(Collectors.toList());
        return orderIds;
    }
}
