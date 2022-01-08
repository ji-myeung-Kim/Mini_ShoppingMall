package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName(); //강제 초기화
            order.getDelivery().getAddress();  //강제 초기화

            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
        }
        return all;
    }

    /**
     * order 안에 orderitem도 dto형태로 받자(절대 entity를 노출시키지 말자!!!)->stream map 사용
     * {
     *         "orderId": 4,
     *         "name": "userA",
     *         "orderdate": "2022-01-08T14:59:38.214003",
     *         "orderStatus": "ORDER",
     *         "address": {
     *             "city": "서울",
     *             "street": "1",
     *             "zipcode": "1111"
     *         },
     *         "orderItems": [
     *             {
     *                 "itemName": "JPA1 Book",
     *                 "orderPrice": 10000,
     *                 "count": 1
     *             },
     *             {
     *                 "itemName": "JPA2 Book",
     *                 "orderPrice": 20000,
     *                 "count": 2
     *             }
     *         ]
     *     }
     *
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return result;
    }

    /**
     * distinct 사용하여 중복제거
     * 페치 조인으로 sql이 1번만 실행 됨
     * Jpa의 distinct는 SQL에 distinct 를 추가하고, 더해서 같은 에티티가 조회되면
     * 애플리케이션에서 중복을 걸러줌, order 가 컬랙션 페치 조인 때문에 중복조회 되는 것을
     * 막아줌
     *
     * 단점 - *******페이지 불가능******
     * warn 내면서 메모리에서 해주긴하지만 모든 데이터들을 상대로 실험해야됨!!!!메모리 아웃됨;;;
     *
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> orders3() {
        List<Order> orders = orderRepository.findAllWithItem();

        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return result;
    }

    /**
     *    select
     *         orderitems0_.order_id as order_id5_5_1_,
     *         orderitems0_.order_item_id as order_it1_5_1_,
     *         orderitems0_.order_item_id as order_it1_5_0_,
     *         orderitems0_.count as count2_5_0_,
     *         orderitems0_.item_id as item_id4_5_0_,
     *         orderitems0_.order_id as order_id5_5_0_,
     *         orderitems0_.order_price as order_pr3_5_0_
     *     from
     *         order_item orderitems0_
     *     where
     *         orderitems0_.order_id in (
     *             ?, ?
     *         )
     *         in 사용함으로써 한번에 다 땡겨옴
     *==============================================================
     *   select
     *         item0_.item_id as item_id2_3_0_,
     *         item0_.name as name3_3_0_,
     *         item0_.price as price4_3_0_,
     *         item0_.stock_quantity as stock_qu5_3_0_,
     *         item0_.artist as artist6_3_0_,
     *         item0_.etc as etc7_3_0_,
     *         item0_.author as author8_3_0_,
     *         item0_.isbn as isbn9_3_0_,
     *         item0_.actor as actor10_3_0_,
     *         item0_.director as directo11_3_0_,
     *         item0_.dtype as dtype1_3_0_
     *     from
     *         item item0_
     *     where
     *         item0_.item_id in (
     *             ?, ?, ?, ?
     *         )
     *
     *  여기도 in 사용함으로써 2, 3, 9, 10 한번에 땡겨올 수 있다.
     *  1:N:M -> 1:1:1
     *
     *  default_batch_fetch_size: 100사용!!!!!
     *
     *  장점:조인보다 Db 데이터 전송량이 최적화됨
     *  (Order와 OrderItem 을 조인하면 order가 OrderItem만큼 중복해서 죄회
     *  이 방법은 각각 조회하므로 전송해야할 중복 데이터가 없다.
     *
     *  페치 조인 방식과 비교해서 쿼리 호출수가 약간 증가하지만, DB 데이터 전송량이 감소
     *  컬렉션 페치 조인은 페이징 불가능 하지만 이 방법은 페이징이 가능
     *
     *  결론 ----
     *  ToOne 관계는 페치 조인해서 페이징에 영향을 주지 않음. 따라서 ToOne 관계는 페치조인으로
     *  쿼리 수를 줄이고 해결하고, 나머지는 hibernate.default_batch_fetch_size로 최적화화
    */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit)
    {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
        return result;
    }

    /**
     *
     * @return
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4 () {
        return orderQueryRepository.findOrderQueryDtos();
    }

    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDtoOptimization();
    }

    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDtoFlat();

        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(),
                                o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(),
                                o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(),
                        e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(),
                        e.getKey().getAddress(), e.getValue()))
                .collect(toList());
    }

    /**
     * no properties라고 뜰경우 있는데 Data가 존재하지 않는 경우
     */
    @Data
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderdate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderdate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(toList());
        }
    }

    @Getter
    static class OrderItemDto {

        private String itemName; //상품명
        private int orderPrice; //주문가격
        private int count; // 주문수량


        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();

        }
    }
}
