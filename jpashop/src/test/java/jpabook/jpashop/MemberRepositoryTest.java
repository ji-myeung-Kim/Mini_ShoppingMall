//package jpabook.jpashop;
//
//import org.assertj.core.api.Assertions;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.annotation.Rollback;
//import org.springframework.test.context.junit4.SpringRunner;
//import org.springframework.transaction.annotation.Transactional;
//
//import static org.junit.Assert.*;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest
//public class MemberRepositoryTest {
//
//    @Autowired MemberRepository memberRepository;
//
//    @Test
////    테스트가 있을경우 rollback을 해버린다 , 새로운 시도를 위해
//    @Transactional
//    @Rollback(false)
//    public void testMember() throws Exception {
//        Member member = new Member();
//        member.setUsername("memberA");
//        Long saveId = memberRepository.save(member);
//        Member findMember = memberRepository.find(saveId);
//
//        Assertions.assertThat(findMember.getId()).isEqualTo(member.getId());
//        Assertions.assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
//        Assertions.assertThat(findMember).isEqualTo(member);
//        System.out.println("findMember == member : " + (findMember == member));
//
//    }
//}
////  같은 트렌젝션안에서 저장 하고 조회하면 영속성 컨테스트가 똑같아 id가 같으면 같은걸로 식별 1차캐시에 있기때문에 select문도 안날림