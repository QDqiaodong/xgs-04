package com.bookexchange.config;

import com.bookexchange.entity.BorrowRule;
import com.bookexchange.entity.Category;
import com.bookexchange.entity.City;
import com.bookexchange.entity.Tag;
import com.bookexchange.entity.User;
import com.bookexchange.entity.UserLevel;
import com.bookexchange.repository.BorrowRuleRepository;
import com.bookexchange.repository.CategoryRepository;
import com.bookexchange.repository.CityRepository;
import com.bookexchange.repository.TagRepository;
import com.bookexchange.repository.UserLevelRepository;
import com.bookexchange.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(CityRepository cityRepository,
                                       CategoryRepository categoryRepository,
                                       UserRepository userRepository,
                                       TagRepository tagRepository,
                                       BorrowRuleRepository borrowRuleRepository,
                                       UserLevelRepository userLevelRepository) {
        return args -> {
            if (cityRepository.count() == 0) {
                List<City> cities = Arrays.asList(
                    createCity("北京市", "北京", "110000"),
                    createCity("上海市", "上海", "310000"),
                    createCity("广东省", "广州", "440100"),
                    createCity("广东省", "深圳", "440300"),
                    createCity("浙江省", "杭州", "330100"),
                    createCity("江苏省", "南京", "320100"),
                    createCity("江苏省", "苏州", "320500"),
                    createCity("四川省", "成都", "510100"),
                    createCity("湖北省", "武汉", "420100"),
                    createCity("陕西省", "西安", "610100"),
                    createCity("山东省", "济南", "370100"),
                    createCity("山东省", "青岛", "370200"),
                    createCity("河南省", "郑州", "410100"),
                    createCity("湖南省", "长沙", "430100"),
                    createCity("福建省", "福州", "350100"),
                    createCity("福建省", "厦门", "350200")
                );
                cityRepository.saveAll(cities);
            }

            if (categoryRepository.count() == 0) {
                List<Category> categories = Arrays.asList(
                    createCategory("文学小说", "包括小说、散文、诗歌等文学作品"),
                    createCategory("人文社科", "历史、哲学、心理学、社会学等"),
                    createCategory("科技科普", "科学技术、科普读物、计算机等"),
                    createCategory("经济管理", "经济学、管理学、商业等"),
                    createCategory("教育学习", "教材、考试、学习方法等"),
                    createCategory("生活艺术", "生活休闲、美食、旅游、艺术等"),
                    createCategory("儿童读物", "儿童文学、绘本、启蒙读物等"),
                    createCategory("其他", "其他类型图书")
                );
                categoryRepository.saveAll(categories);
            }

            if (userRepository.count() == 0) {
                City beijing = cityRepository.findByCityNameContaining("北京").get(0);
                City shanghai = cityRepository.findByCityNameContaining("上海").get(0);

                List<User> users = Arrays.asList(
                    createUser("zhangsan", "张三", "13800138001", beijing),
                    createUser("lisi", "李四", "13800138002", shanghai),
                    createUser("wangwu", "王五", "13800138003", beijing)
                );
                userRepository.saveAll(users);
            }

            if (tagRepository.count() == 0) {
                List<Tag> tags = Arrays.asList(
                    createTag("科幻", "科幻类作品", "#409EFF"),
                    createTag("经典", "经典文学作品", "#67C23A"),
                    createTag("必读", "推荐必读图书", "#E6A23C"),
                    createTag("畅销", "热门畅销图书", "#F56C6C"),
                    createTag("新书", "新上架图书", "#909399"),
                    createTag("人文", "人文社科类", "#8E44AD"),
                    createTag("历史", "历史类作品", "#D35400"),
                    createTag("悬疑", "悬疑推理类", "#2C3E50")
                );
                tagRepository.saveAll(tags);
            }

            if (borrowRuleRepository.count() == 0) {
                BorrowRule rule = new BorrowRule();
                rule.setMaxBorrowCount(5);
                rule.setMaxBorrowDays(30);
                rule.setReservationHours(48);
                rule.setAllowRenew(true);
                rule.setMaxRenewCount(2);
                rule.setDescription("系统默认借阅规则");
                borrowRuleRepository.save(rule);
            }

            if (userLevelRepository.count() == 0) {
                List<UserLevel> levels = Arrays.asList(
                    createUserLevel("BRONZE", "青铜", 0, 3, 1, "🥉", "初始等级，可借阅3本图书"),
                    createUserLevel("SILVER", "白银", 100, 5, 2, "🥈", "白银等级，可借阅5本图书"),
                    createUserLevel("GOLD", "黄金", 500, 8, 3, "🥇", "黄金等级，可借阅8本图书"),
                    createUserLevel("DIAMOND", "钻石", 1500, 12, 4, "💎", "钻石等级，可借阅12本图书")
                );
                userLevelRepository.saveAll(levels);
            }
        };
    }

    private City createCity(String province, String cityName, String cityCode) {
        City city = new City();
        city.setProvince(province);
        city.setCityName(cityName);
        city.setCityCode(cityCode);
        return city;
    }

    private Category createCategory(String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        return category;
    }

    private User createUser(String username, String nickname, String phone, City city) {
        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setCity(city);
        return user;
    }

    private Tag createTag(String name, String description, String color) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setDescription(description);
        tag.setColor(color);
        return tag;
    }

    private UserLevel createUserLevel(String code, String name, int minPoints, int maxBorrowCount, int sortOrder, String icon, String description) {
        UserLevel level = new UserLevel();
        level.setCode(code);
        level.setName(name);
        level.setMinPoints(minPoints);
        level.setMaxBorrowCount(maxBorrowCount);
        level.setSortOrder(sortOrder);
        level.setIcon(icon);
        level.setDescription(description);
        return level;
    }
}
