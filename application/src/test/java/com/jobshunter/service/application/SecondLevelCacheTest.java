package com.jobshunter.service.application;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.RoleEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.repository.AiModelRepository;
import com.jobshunter.database.repository.AiModelsCapabilityRepository;
import com.jobshunter.database.repository.RoleRepository;
import com.jobshunter.database.repository.UserRepository;
import com.jobshunter.model.EngineType;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:cachetest;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.defer-datasource-initialization=true",
        "spring.liquibase.enabled=false",
        "spring.sql.init.mode=always",
        "spring.sql.init.data-locations=classpath:data.sql",
        // Enable Hibernate statistics
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.cache.use_structured_entries=true"
    }
)
@Import(SqlTestDataInitializer.class)
@ActiveProfiles("test")
class SecondLevelCacheTest {

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AiModelRepository aiModelRepository;

  @Autowired
  private AiModelsCapabilityRepository aiModelsCapabilityRepository;

  private Statistics statistics;

  @BeforeEach
  void setUp() {
    statistics = entityManager.unwrap(Session.class)
        .getSessionFactory()
        .getStatistics();
    statistics.setStatisticsEnabled(true);
  }

  // ──────────────────────────────────────────────
  //  Helper: clears L1 cache so we only test L2
  // ──────────────────────────────────────────────
  private void evictL1Cache() {
    entityManager.clear();
  }

  private void logCacheStats(String label) {
    log.info("""
        
        ====== {} ======
          L2 Entity  hits  : {}
          L2 Entity  misses: {}
          L2 Entity  puts  : {}
          Query cache hits  : {}
          Query cache misses: {}
          Query cache puts  : {}
          Total queries executed: {}
        ========================
        """,
        label,
        statistics.getSecondLevelCacheHitCount(),
        statistics.getSecondLevelCacheMissCount(),
        statistics.getSecondLevelCachePutCount(),
        statistics.getQueryCacheHitCount(),
        statistics.getQueryCacheMissCount(),
        statistics.getQueryCachePutCount(),
        statistics.getQueryExecutionCount()
    );
  }

  // ═══════════════════════════════════════════════
  //  TEST 1.1: roleRepository#findById() covered by entity cache
  //  Proves: entity cache works without query hints
  // ═══════════════════════════════════════════════
  @Test
  @DisplayName("roleRepository#findById() should be served from L2 entity cache without @QueryHints")
  void findById_shouldUseEntityCache() {
    RoleEntity role = new RoleEntity();
    role.setName("R1_findById");
    role = roleRepository.save(role);
    Long roleId = role.getId();
    evictL1Cache();

    // --- FIRST CALL: populates entity cache ---
    statistics.clear();
    roleRepository.findById(roleId);
    logCacheStats("findById - After FIRST call");

    evictL1Cache();

    // --- SECOND CALL: should come from entity cache ---
    statistics.clear();
    var cached = roleRepository.findById(roleId);
    assertThat(cached).isPresent();
    logCacheStats("findById - After SECOND call (expect entity cache HIT)");

    assertThat(statistics.getSecondLevelCacheHitCount())
            .as("Entity should be served from L2 cache")
            .isGreaterThan(0);
    assertThat(statistics.getQueryExecutionCount())
            .as("No query should be executed")
            .isEqualTo(0);
  }

  // ═══════════════════════════════════════════════
  //  TEST 1.2: roleRepository#findByName()
  //  Proves: entity cache + query cache working
  // ═══════════════════════════════════════════════
  @Test
  @DisplayName("RoleRepository.findByName() should hit L2 cache on second call")
  void roleRepository_findByName_shouldUseCache() {
    // --- Seed data ---
    RoleEntity role = new RoleEntity();
    role.setName("R2_findByName");
    roleRepository.save(role);
    evictL1Cache();

    // --- FIRST CALL: cache miss, populates cache ---
    statistics.clear();
    var result1 = roleRepository.findByName("R2_findByName");
    assertThat(result1).isPresent();
    logCacheStats("Role - After FIRST call (expect MISS)");

    long queriesAfterFirst = statistics.getQueryExecutionCount();
    assertThat(queriesAfterFirst).as("First call should hit the DB").isGreaterThan(0);

    // --- Clear L1 cache so second call MUST go through L2 ---
    evictL1Cache();

    // --- SECOND CALL: should be fully served from L2 cache ---
    statistics.clear();
    var result2 = roleRepository.findByName("R2_findByName");
    assertThat(result2).isPresent();
    logCacheStats("Role - After SECOND call (expect HIT)");

    long queriesAfterSecond = statistics.getQueryExecutionCount();
    long cacheHitsAfterSecond = statistics.getSecondLevelCacheHitCount()
        + statistics.getQueryCacheHitCount();

    assertThat(queriesAfterSecond)
        .as("Second call should execute ZERO SQL queries (served from cache)")
        .isEqualTo(0);
    assertThat(cacheHitsAfterSecond)
        .as("Second call should have cache hits")
        .isGreaterThan(0);
  }

  // ═══════════════════════════════════════════════
  //  TEST 3.1: userRepository#findByUsername()
  //  Proves: entity cache on User + query cache
  // ═══════════════════════════════════════════════
  @Test
  @DisplayName("userRepository#findByUsername() should hit cache on second call")
  void userRepository_findByUsername_shouldUseCache() {
    // --- Seed user ---
    RoleEntity role = roleRepository.findByName("USER")
            .orElseGet(() -> {
              RoleEntity r = new RoleEntity();
              r.setName("USER");
              return roleRepository.save(r);
            });

    UserEntity user = new UserEntity();
    user.setUsername("cache_test_user");
    user.setEmail("cache@test.com");
    user.setPassword("hashed_password");
    user.setPhoneNumber("+40700000000");
    user.setRoles(Set.of(role));
    userRepository.save(user);
    evictL1Cache();

    // --- FIRST CALL ---
    statistics.clear();
    var result1 = userRepository.findByUsername("cache_test_user");
    assertThat(result1).isPresent();
    logCacheStats("User - After FIRST call (expect MISS)");

    long queriesFirst = statistics.getQueryExecutionCount();
    assertThat(queriesFirst).isGreaterThan(0);

    evictL1Cache();

    // --- SECOND CALL ---
    statistics.clear();
    var result2 = userRepository.findByUsername("cache_test_user");
    assertThat(result2).isPresent();
    logCacheStats("User - After SECOND call (expect HIT)");

    assertThat(statistics.getQueryExecutionCount())
            .as("No SQL should be executed on second call")
            .isEqualTo(0);
    assertThat(statistics.getSecondLevelCacheHitCount() + statistics.getQueryCacheHitCount())
            .as("Should have L2 cache hits")
            .isGreaterThan(0);
  }

  // ═══════════════════════════════════════════════
  //  TEST 3.2: Role collection
  // ═══════════════════════════════════════════════
  @Test
  @DisplayName("role collection should hit L2 cache on second call")
  void rolecollection_shouldUseCache() {
    // --- Seed data ---
    RoleEntity role = new RoleEntity();
    role.setName("CACHE_TEST_ROLE");
    roleRepository.save(role);
    UserEntity userEntity = new UserEntity();
    userEntity.getRoles().add(role);
    userEntity.setUsername("user1");
    userEntity.setEmail("user1@yahoo.com");
    userEntity.setPassword("password1");
    userEntity.setPhoneNumber("0771111111");
    UserEntity userEntity1 = userRepository.saveAndFlush(userEntity);
    System.out.println("userEntity1:"+userEntity1);
    evictL1Cache();

    // --- FIRST CALL: cache miss, populates cache ---
    statistics.clear();
    var result1 = userRepository.findByUsername("user1");
    assertThat(result1).isPresent();
    logCacheStats("user1 - After FIRST call (expect MISS)");

    long queriesAfterFirst = statistics.getQueryExecutionCount();
    assertThat(queriesAfterFirst).as("First call should hit the DB").isGreaterThan(0);

    // --- Clear L1 cache so second call MUST go through L2 ---
    evictL1Cache();

    // --- SECOND CALL: should be fully served from L2 cache ---
    statistics.clear();
    var result2 = userRepository.findByUsername("user1");
    assertThat(result2).isPresent();
    System.out.println("##roles:"+userEntity1.getRoles());
    logCacheStats("user1 - After SECOND call (expect HIT)");

    long queriesAfterSecond = statistics.getQueryExecutionCount();
    long cacheHitsAfterSecond = statistics.getSecondLevelCacheHitCount()
            + statistics.getQueryCacheHitCount();

    assertThat(queriesAfterSecond)
            .as("Second call should execute ZERO SQL queries (served from cache)")
            .isEqualTo(0);
    assertThat(cacheHitsAfterSecond)
            .as("Second call should have cache hits")
            .isGreaterThan(0);

  }

  // ═══════════════════════════════════════════════
  //  TEST 4: AiModelRepository read methods
  //  Proves: query cache on findByProviderAndModel
  // ═══════════════════════════════════════════════
  @Test
  @DisplayName("AiModelRepository.findByProviderAndModel() should hit cache on second call")
  void aiModelRepository_findByProviderAndModel_shouldUseCache() {
    // --- Seed data (data.sql already inserts models, but let's be explicit) ---
    aiModelRepository.findByProviderAndModel(EngineType.GEMINI, "gemini-2.5-flash-lite")
        .orElseGet(() -> aiModelRepository.save(new AiModelEntity(EngineType.GEMINI, "gemini-2.5-flash-lite")));
    evictL1Cache();

    // --- FIRST CALL ---
    statistics.clear();
    var result1 = aiModelRepository.findByProviderAndModel(EngineType.GEMINI, "gemini-2.5-flash-lite");
    assertThat(result1).isPresent();
    logCacheStats("AiModel - After FIRST call (expect MISS)");

    long queriesFirst = statistics.getQueryExecutionCount();
    assertThat(queriesFirst).isGreaterThan(0);

    evictL1Cache();

    // --- SECOND CALL ---
    statistics.clear();
    var result2 = aiModelRepository.findByProviderAndModel(EngineType.GEMINI, "gemini-2.5-flash-lite");
    assertThat(result2).isPresent();
    logCacheStats("AiModel - After SECOND call (expect HIT)");

    assertThat(statistics.getQueryExecutionCount())
        .as("No SQL should be executed on second call")
        .isEqualTo(0);
  }

  // ═══════════════════════════════════════════════
  //  TEST 5: Stress test -- prove performance gain
  //  Measures wall-clock time over many iterations
  // ═══════════════════════════════════════════════
  @Test
  @DisplayName("100 repeated findByName() calls should be fast thanks to cache")
  void stressTest_repeatedCalls_shouldBeFast() {
    RoleEntity role = roleRepository.findByName("USER")
        .orElseGet(() -> {
          RoleEntity r = new RoleEntity();
          r.setName("USER");
          return roleRepository.save(r);
        });
    evictL1Cache();

    // Warm up cache
    roleRepository.findByName("USER");
    evictL1Cache();

    statistics.clear();

    long start = System.nanoTime();
    for (int i = 0; i < 100; i++) {
      evictL1Cache(); // force L2 usage, not L1
      roleRepository.findByName("USER");
    }
    long durationMs = (System.nanoTime() - start) / 1_000_000;

    logCacheStats("Stress test - 100 calls");

    log.info("100 cached findByName() calls completed in {}ms", durationMs);
    log.info("Cache hit ratio: {}%",
        statistics.getQueryCacheHitCount() * 100
            / Math.max(1, statistics.getQueryCacheHitCount() + statistics.getQueryCacheMissCount()));

    // After warmup, 99 of 100 calls should be cache hits
    assertThat(statistics.getQueryCacheHitCount())
        .as("Most calls should be query cache hits")
        .isGreaterThanOrEqualTo(99);
    assertThat(statistics.getQueryExecutionCount())
        .as("At most 1 SQL query (the initial miss)")
        .isLessThanOrEqualTo(1);
  }

}