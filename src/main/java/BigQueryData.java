import com.abjadiyat.entity.PurchaseRecords;
import com.abjadiyat.util.HibernateUtil;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.*;
import org.hibernate.HibernateException;
import org.hibernate.Session;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


public class BigQueryData {
    static Session sessionObj;

    public static void main(String... args) throws Exception {
        sessionObj = HibernateUtil.getSessionFactory().openSession();
        sessionObj.beginTransaction();

        File credentialsPath = new File("src/main/resources/Abjadiyat-67b7648019f7.json");
        BigQuery bigquery = BigQueryOptions.newBuilder().setProjectId("abjadiyat-167307")
                .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(credentialsPath))
                ).build().getService();

        Job queryJob = getBigQueryJob(bigquery);

        TableResult result = queryJob.getQueryResults();

        for (FieldValueList row : result.iterateAll()) {
            String eventName = row.get("event_name").getStringValue();
            String osName = row.get("os_name").getStringValue();
            String osVersion = row.get("os_version").getStringValue();
            Long userId = row.get("user_id").getLongValue();
            PurchaseRecords purchaseRecords = PurchaseRecords.builder()
                    .userId(userId)
                    .eventName(eventName)
                    .osName(osName)
                    .osVersion(osVersion)
                    .build();
            try {
                sessionObj.saveOrUpdate(purchaseRecords);
            } catch (HibernateException hex) {
                System.out.println(hex.getMessage());

            }
        }
        sessionObj.getTransaction().commit();
    }

    private static Job getBigQueryJob(BigQuery bigquery) throws InterruptedException {
        LocalDate localDate = LocalDate.now().minusDays(1L);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String eventDate = localDate.format(formatter);

        String query = " SELECT user_id, device.operating_system as os_name, device.operating_system_version as os_version," +
                " event_name FROM analytics_164337572.events_"+eventDate+",UNNEST(event_params) AS ep WHERE event_name in ('PurchaseSuccess'," +
                "'PurchaseFailure', 'PurchaseCancelledByUser') AND ep.key = 'productId'" +
                " GROUP BY user_id, os_name, os_version, event_name ";

        QueryJobConfiguration queryConfig =
                QueryJobConfiguration.newBuilder(query)
                        .setUseLegacySql(false)
                        .build();

        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
        queryJob = queryJob.waitFor();

        if (queryJob == null) {
            throw new RuntimeException("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {

            throw new RuntimeException(queryJob.getStatus().getError().toString());
        }
        return queryJob;
    }
}
