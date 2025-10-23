package com.smartcane.point.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

@Configuration
public class FlywayRepairConfig {
    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            // V1 같은 “로컬에 없는” 기록을 삭제 상태로 정리
            flyway.repair();
            // 그 다음 정상 마이그레이션 수행
            flyway.migrate();
        };
    }
}
