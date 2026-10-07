package com.apnabazaar.service;
import com.apnabazaar.dto.IngestionResult;
import com.apnabazaar.entity.*;
import com.apnabazaar.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/**
 * Reads daily menu Excel uploads.
 * Columns: A=SellerName B=ShopName C=FlatNo D=WhatsApp
 *          E=ItemName F=Price G=PickupTime H=PickupLocation
 *          I=Delivery J=Qty K=Notes L=PostDate
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelIngestionService {
    private final ProviderRepository providerRepo;
    private final DailyPostRepository dailyPostRepo;
    private final CommunityRepository communityRepo;

    @Transactional
    public IngestionResult ingestDailyMenu(MultipartFile file, String communitySlug) throws IOException {
        Community community = communityRepo.findBySlug(communitySlug)
            .orElseThrow(() -> new RuntimeException("Community not found: " + communitySlug));
        Workbook wb = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = wb.getSheetAt(0);

        Map<String, SellerGroup> groups = new LinkedHashMap<>();
        int rowsProcessed = 0, rowsSkipped = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isBlank(row)) continue;
            try {
                String name = str(row, 0), wa = str(row, 3);
                if (name.isBlank() && wa.isBlank()) continue;
                String key = wa.isBlank() ? name.toLowerCase() : wa;
                groups.computeIfAbsent(key, k -> new SellerGroup(name, str(row,1), str(row,2), wa, parseDate(row,11))).addRow(row);
                rowsProcessed++;
            } catch (Exception e) { errors.add("Row " + (i+1) + ": " + e.getMessage()); rowsSkipped++; }
        }

        int providersUpdated = 0, itemsCreated = 0;
        for (SellerGroup g : groups.values()) {
            try {
                Provider p = upsert(community, g);
                itemsCreated += createPost(p, g);
                providersUpdated++;
            } catch (Exception e) { errors.add("Seller '" + g.name + "': " + e.getMessage()); log.error("", e); }
        }
        wb.close();
        log.info("Ingested: {} rows, {} providers, {} items, {} errors",
            rowsProcessed, providersUpdated, itemsCreated, errors.size());
        return new IngestionResult(rowsProcessed, rowsSkipped, providersUpdated, itemsCreated, errors);
    }

    private Provider upsert(Community community, SellerGroup g) {
        if (!g.wa.isBlank()) {
            Optional<Provider> byWa = providerRepo.findByCommunityIdAndWhatsappNumber(community.getId(), g.wa);
            if (byWa.isPresent()) return byWa.get();
        }
        return providerRepo.findByCommunityIdAndStatus(community.getId(), Provider.ProviderStatus.active).stream()
            .filter(p -> p.getName().equalsIgnoreCase(g.name)).findFirst()
            .orElseGet(() -> providerRepo.save(Provider.builder()
                .community(community).name(g.name)
                .shopName(g.shop.isBlank() ? g.name : g.shop)
                .flatNumber(g.flat).whatsappNumber(g.wa)
                .providerType(Provider.ProviderType.food_seller)
                .status(Provider.ProviderStatus.active).build()));
    }

    private int createPost(Provider provider, SellerGroup g) {
        dailyPostRepo.findByProviderIdAndPostDate(provider.getId(), g.date)
            .forEach(dp -> { dp.setIsActive(false); dailyPostRepo.save(dp); });
        DailyPost post = DailyPost.builder()
            .provider(provider).postDate(g.date)
            .source(DailyPost.PostSource.excel_upload).isActive(true).build();
        List<DailyLineItem> items = g.rows.stream()
            .filter(r -> !str(r,4).isBlank())
            .map(r -> DailyLineItem.builder()
                .dailyPost(post).itemName(str(r,4))
                .price(num(r,5)).pickupTime(str(r,6)).pickupLocation(str(r,7))
                .deliveryType(delivery(str(r,8))).quantityAvailable(intVal(r,9)).notes(str(r,10))
                .build()).toList();
        post.setLineItems(new ArrayList<>(items));
        dailyPostRepo.save(post);
        return items.size();
    }

    private String str(Row row, int col) {
        Cell c = row.getCell(col);
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(c)
                ? c.getLocalDateTimeCellValue().toLocalTime().toString()
                : String.valueOf((long) c.getNumericCellValue());
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default -> "";
        };
    }
    private BigDecimal num(Row r, int c) { try { String s=str(r,c); return s.isBlank()?null:new BigDecimal(s); } catch (Exception e){return null;} }
    private Integer intVal(Row r, int c) { try { String s=str(r,c); return s.isBlank()?null:Integer.parseInt(s); } catch (Exception e){return null;} }
    private LocalDate parseDate(Row row, int col) {
        Cell c = row.getCell(col);
        if (c != null && c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c))
            return c.getLocalDateTimeCellValue().toLocalDate();
        return LocalDate.now(ZoneId.of("Asia/Kolkata"));
    }
    private boolean isBlank(Row row) {
        for (int i=0;i<=11;i++){Cell c=row.getCell(i); if(c!=null&&c.getCellType()!=CellType.BLANK&&!c.toString().trim().isEmpty()) return false;} return true;
    }
    private DailyLineItem.DeliveryType delivery(String s) {
        if (s==null) return DailyLineItem.DeliveryType.pickup;
        return switch (s.toLowerCase().trim()) { case "home delivery","delivery","both" -> DailyLineItem.DeliveryType.both; default -> DailyLineItem.DeliveryType.pickup; };
    }

    private static class SellerGroup {
        String name, shop, flat, wa; LocalDate date; List<Row> rows = new ArrayList<>();
        SellerGroup(String n, String s, String fl, String w, LocalDate d){name=n;shop=s;flat=fl;wa=w;date=d;}
        void addRow(Row r){rows.add(r);}
    }
}
