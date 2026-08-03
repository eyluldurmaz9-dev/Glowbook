package glowbook.config;

import glowbook.entity.ServiceOption;
import glowbook.entity.ServicePackage;
import glowbook.repository.ServiceOptionRepository;
import glowbook.repository.ServicePackageRepository;
import glowbook.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
public class CatalogSeedConfig {

    private final ServiceRepository serviceRepository;
    private final ServiceOptionRepository serviceOptionRepository;
    private final ServicePackageRepository servicePackageRepository;

    @Bean
    ApplicationRunner seedCatalog() {
        return args -> seedIfEmpty();
    }

    @Transactional
    void seedIfEmpty() {
        if (serviceRepository.count() > 0) {
            return;
        }

        glowbook.entity.Service skinCare = createService(
                "Cilt Bakimi",
                "Cilt analizi, derin temizlik ve nem bakimi ile GlowBook'un imza bakim deneyimi.",
                "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?auto=format&fit=crop&w=900&q=80"
        );
        createOption(skinCare, "Klasik Cilt Bakimi", 900.0);
        createOption(skinCare, "Hydrafacial Bakim", 1500.0);
        createPackage(skinCare, "Glow Cilt Paketi", "4 seanslik yenileyici cilt bakimi paketi.", 4, 5200.0);

        glowbook.entity.Service laser = createService(
                "Lazer Epilasyon",
                "Konforlu randevu akisiyla bolge bazli lazer epilasyon hizmetleri.",
                "https://images.unsplash.com/photo-1515377905703-c4788e51af15?auto=format&fit=crop&w=900&q=80"
        );
        createOption(laser, "Tum Vucut", 2200.0);
        createOption(laser, "Yuz Bolgesi", 650.0);
        createPackage(laser, "Lazer Devam Paketi", "6 seanslik avantajli lazer epilasyon paketi.", 6, 11500.0);

        glowbook.entity.Service massage = createService(
                "Masaj ve Spa",
                "Rahatlatan masaj seanslari ve spa bakimlari.",
                "https://images.unsplash.com/photo-1540555700478-4be289fbecef?auto=format&fit=crop&w=900&q=80"
        );
        createOption(massage, "Aromaterapi Masaji", 1200.0);
        createOption(massage, "Medikal Masaj", 1450.0);
        createPackage(massage, "Spa Yenilenme Paketi", "3 seanslik masaj ve spa paketi.", 3, 3600.0);
    }

    private glowbook.entity.Service createService(String name, String description, String image) {
        return serviceRepository.save(glowbook.entity.Service.builder()
                .serviceName(name)
                .description(description)
                .serviceImage(image)
                .active(true)
                .build());
    }

    private void createOption(glowbook.entity.Service service, String name, Double price) {
        serviceOptionRepository.save(ServiceOption.builder()
                .service(service)
                .optionName(name)
                .price(price)
                .active(true)
                .build());
    }

    private void createPackage(glowbook.entity.Service service, String name, String description, Integer sessions, Double price) {
        servicePackageRepository.save(ServicePackage.builder()
                .service(service)
                .packageName(name)
                .description(description)
                .totalSession(sessions)
                .price(price)
                .active(true)
                .build());
    }
}
