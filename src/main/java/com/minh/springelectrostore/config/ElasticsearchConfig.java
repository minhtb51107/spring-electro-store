package com.minh.springelectrostore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.lang.NonNull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.minh.springelectrostore.search.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

	@Value("${spring.elasticsearch.uris}")
    private String uris; // Lấy từ biến môi trường: http://elasticsearch:9200

    @Value("${spring.elasticsearch.username}")
    private String username; // elastic

    @Value("${spring.elasticsearch.password}")
    private String password; // 511007

    @Override
    @NonNull
    public ClientConfiguration clientConfiguration() {
        // Xử lý chuỗi URI để lấy host:port (loại bỏ http:// hoặc https://)
        String target = uris.replace("http://", "").replace("https://", "");

        return ClientConfiguration.builder()
                .connectedTo(target) // Kết nối tới "elasticsearch:9200" trong Docker
                .withBasicAuth(username, password) // Bắt buộc phải có dòng này vì xpack enabled
                .withConnectTimeout(Duration.ofSeconds(10))
                .withSocketTimeout(Duration.ofSeconds(10))
                .build();
    }

    // Hàm helper để tạo SSL Context chấp nhận mọi chứng chỉ (Dùng cho Dev)
    private SSLContext buildTrustAllSslContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            return sc;
        } catch (Exception e) {
            throw new RuntimeException("Không thể khởi tạo SSL Context", e);
        }
    }
}