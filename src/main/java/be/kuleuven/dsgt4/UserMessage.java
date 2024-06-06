package be.kuleuven.dsgt4;

import java.time.LocalDateTime;
import java.util.UUID;
import java.io.Serializable;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
public class UserMessage {



        private UUID id; //UUID类型，用于唯一标识消息
        private LocalDateTime time;
        private String role;
        private String customer; //String 类型，用于表示客户信息:email

        public UserMessage(UUID id, LocalDateTime time, String role, String customer) {
            this.id = id;
            this.time = time;
            this.role = role;
            this.customer = customer;
        }

        public UUID getId() {
            return this.id;
        }

        public LocalDateTime getTime() {
            return this.time;
        }

        public String getCustomer() {
            return this.customer;
        }

        //将 UserMessage 对象转换为 Map<String, Object> 格式，便于存储或传输
        public Map<String, Object> toDoc(){
            Map<String, Object> data = new HashMap<>();
            data.put("id", this.id.toString());
            data.put("time", this.time.format(DateTimeFormatter.ISO_DATE_TIME));
            data.put("role", this.role.toString());
            data.put("customer", this.customer.toString());


            return data;
        }

        //从 Map<String, Object> 文档创建 UserMessage 对象
        public static UserMessage fromDoc(Map<String, Object> doc) {
            return new UserMessage(
                    UUID.fromString((String) doc.get("id")),
                    LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse((String) doc.get("time"))),
                    (String) doc.get("role"),
                    (String) doc.get("customer"));
        }
    }
