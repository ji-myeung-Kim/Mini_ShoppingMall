package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

//Contoller + ResponseBody -> data 자체를 xml이나 json 형식으로 보내자~
@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @GetMapping("/api/v1/members")
    public List<Member> membersV1() {
        return memberService.findMembers();
    }

    /**
     * entity가 변하더라고 api 스펙은 변하지 않음
     */
    @GetMapping("/api/v2/members")
    public Result memberV2() {
        List<Member> findMembers = memberService.findMembers();
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName(),m.getAddress()))
                .collect(Collectors.toList());

        return new Result(collect.size(), collect);
    }

    /**
     * Data로 감싸주면 유연성 증가
     */
    @Data
    @AllArgsConstructor
    static class Result<T> {
        private int count;
        private T data;
    }

    /**
     * 원하는 값들 조절 가능
     */
    @Data
    @AllArgsConstructor
    static class MemberDto {
        private String name;
        private Address address;
    }




    @PostMapping("/api/v1/members")
//    이름을 필수값으로 하고 싶을때 valid사용 엔티티에서 이름 위에 NotEmpty 적용
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /**
    절대 entity를 사용하지 않는다!!!!!들어오고 나갈때
     **/
    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMembverV2(@RequestBody @Valid CreateMemberRequest request){

        Member member = new Member();
        member.setName(request.name);

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(
            @PathVariable("id") Long id,
            @RequestBody UpdateMemberRequest request) {

        memberService.update(id, request.getName());
        Member findMember = memberService.findOne(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    @Data
    static class UpdateMemberRequest{
        private String name;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse{
        private Long id;
        private String name;
    }

    @Data
    static class CreateMemberRequest{
        private String name;
    }
    @Data
    static class CreateMemberResponse{
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id=id ;
        }
    }

}
