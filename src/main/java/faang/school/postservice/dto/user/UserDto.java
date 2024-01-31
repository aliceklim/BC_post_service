package faang.school.postservice.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private List<Long> followerIds;
    private List<Long> followeeIds;
    private String phone;
    private String aboutMe;
    private boolean active;
    private String city;
    private Integer experience;
    private List<Long> mentors;
    private List<Long> mentees;
    private CountryDto country;
    private List<GoalDto> goals;
    private List<SkillDto> skills;
    private PreferredContact preference;
    public enum PreferredContact {
        EMAIL, SMS, TELEGRAM
    }
}
