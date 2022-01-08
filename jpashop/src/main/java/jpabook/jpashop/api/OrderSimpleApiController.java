package jpabook.jpashop.api;


import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * xToOne(ManyToOne, OnetoOne)
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for(Order order : all) {
            order.getMember().getName(); //Lazy 강제 초기화
            order.getDelivery().getAddress(); //Lazy 강제 초기화
        }
        return all;
    }

    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        //ORDER 2개
        //N + 1 -> 1 + 회원 N + 배송 N
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());

        //2개
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return result;
    }

    /**
     * fetch join으로 쿼리 한방으로 해결!!!!
     * entity를 찍는다는 단점이 있긴하다...
     * v4에서 더 최적화를 시켜보자...
     *
     * =====Query=====
     *
     * select
     *         order0_.order_id as order_id1_6_0_,
     *         member1_.member_id as member_i1_4_1_,
     *         delivery2_.delivery_id as delivery1_2_2_,
     *         order0_.delivery_id as delivery4_6_0_,
     *         order0_.member_id as member_i5_6_0_,
     *         order0_.order_date as order_da2_6_0_,
     *         order0_.status as status3_6_0_,
     *         member1_.city as city2_4_1_,
     *         member1_.street as street3_4_1_,
     *         member1_.zipcode as zipcode4_4_1_,
     *         member1_.name as name5_4_1_,
     *         delivery2_.city as city2_2_2_,
     *         delivery2_.street as street3_2_2_,
     *         delivery2_.zipcode as zipcode4_2_2_,
     *         delivery2_.status as status5_2_2_
     *     from
     *         orders order0_
     *     inner join
     *         member member1_
     *             on order0_.member_id=member1_.member_id
     *     inner join
     *         delivery delivery2_
     *             on order0_.delivery_id=delivery2_.delivery_id
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
         List<Order> orders = orderRepository.findAllWithMemberDelivery();

        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return result;
    }

    /**V4
     * 쿼리문에서 내가 원하는 쿼리문만 나갈수가 있음
     * new 명령어를 사용해서 JPQL의 결과를 DTO로 즉시 변환
     * SELECT절에서 원하는 데이터를 지겆ㅂ 선택하므로 DB -> 애플리케이션 네트웍 용량 최적화(생각보다 미비)
     * 리포지토리 재사용성 떨어짐, API 스펙에 맞추누 코드가 레포지토리에 들어가는 단점
     *
     *select
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
     *
     */

    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> orderV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }


    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); //Lazy 초기화
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();//Lazy 초기화
        }
    }

    /**
     * 정리
     *
     * 엔티티를 DTO로 변환하거나, DTO로 바로 조회하는 두가지 방법은 각자 장단점이 있음
     * 둘중 상황에 따라서 더 나은 방법을 선택, 엔티티로 조회하면 레포지토리 재사용성도 좋고
     * 개발도 산순, 따라서 권장하는 방법
     *
     * 1. 우선 엔티티를 DTO로 변환하는 방법을 선택
     * 2. 필요하며 패치 조인으로 성능을 최적화 -> 대부분의 성능 이슈가 해결
     * 3. 그래도 안되면 dTO로 직접 조회하는 방법을 선택
     * 4. 최후의 방법은 JPA가 제공하는 네이티브 SQL 이나 스프링 JDBC TEMPLATE를 사용해서
     * sql 직접 사용
     */
}
