package com.opsdesk.opsdeskbackend.seed;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.opsdesk.opsdeskbackend.project.Project;
import com.opsdesk.opsdeskbackend.project.ProjectRepository;
import com.opsdesk.opsdeskbackend.project.ProjectStatus;
import com.opsdesk.opsdeskbackend.user.AppUser;
import com.opsdesk.opsdeskbackend.user.AppUserRepository;
import com.opsdesk.opsdeskbackend.user.UserRole;

// Runs once when the app starts. Adds sample projects and users to an empty
// database so the frontend has something to show. Data that is already there
// is left alone.
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ProjectRepository projectRepository;
    private final AppUserRepository appUserRepository;

    public DataSeeder(ProjectRepository projectRepository, AppUserRepository appUserRepository) {
        this.projectRepository = projectRepository;
        this.appUserRepository = appUserRepository;
    }

    @Override
    public void run(String... args) {
        if (projectRepository.count() == 0) {
            projectRepository.saveAll(List.of(
                    new Project("Laptop refresh for the sales team", "Amara Okafor",
                            ProjectStatus.IN_PROGRESS, LocalDate.of(2026, 11, 20)),
                    new Project("Office Wi-Fi upgrade", "Thabo Molefe",
                            ProjectStatus.PLANNING, LocalDate.of(2026, 12, 11)),
                    new Project("Move email to the new server", "Lerato Kgosi",
                            ProjectStatus.ON_HOLD, LocalDate.of(2027, 1, 29)),
                    new Project("Printer replacement on floor 2", "Amara Okafor",
                            ProjectStatus.COMPLETE, LocalDate.of(2026, 10, 2))));
            log.info("Seeded {} projects", projectRepository.count());
        }
        if (appUserRepository.count() == 0) {
            Instant joined = Instant.parse("2026-09-01T09:00:00.000Z");
            appUserRepository.saveAll(List.of(
                    new AppUser("Amara Okafor", "amara.okafor@example.com",
                            "IT Support Lead", UserRole.ADMIN, joined),
                    new AppUser("Thabo Molefe", "thabo.molefe@example.com",
                            "Network Engineer", UserRole.AGENT, joined),
                    new AppUser("Lerato Kgosi", "lerato.kgosi@example.com",
                            "Systems Administrator", UserRole.AGENT, joined),
                    new AppUser("Kabelo Dube", "kabelo.dube@example.com",
                            "", UserRole.AGENT, joined)));
            log.info("Seeded {} users", appUserRepository.count());
        }
    }
}
