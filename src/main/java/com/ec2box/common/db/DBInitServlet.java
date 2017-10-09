/**
 * Copyright 2013 Sean Kavanagh - sean.p.kavanagh6@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ec2box.common.db;

import com.ec2box.common.util.AppConfig;
import com.ec2box.manage.model.Auth;
import com.ec2box.manage.util.DBUtils;
import com.ec2box.manage.util.EncryptionUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

/**
 * Initial startup task.  Creates an H2 DB.
 */
@WebServlet(name = "DBInitServlet",
        urlPatterns = {"/config"},
        loadOnStartup = 1)
public class DBInitServlet extends javax.servlet.http.HttpServlet {

    private static Logger log = LoggerFactory.getLogger(DBInitServlet.class);
    /**
     * task init method that created DB
     *
     * @param config task config
     * @throws ServletException
     */
    public void init(ServletConfig config) throws ServletException {

        super.init(config);


        Connection connection = null;
        Statement statement = null;

        //if DB password is empty generate a random
        if(StringUtils.isEmpty(AppConfig.getProperty("dbPassword"))) {
            String dbPassword = null;
            String dbPasswordConfirm = null;
            if(!"true".equals(System.getProperty("GEN_DB_PASS"))) {
                //prompt for password and confirmation
                while (dbPassword == null || !dbPassword.equals(dbPasswordConfirm)) {
                    if (System.console() == null) {
                        Scanner in = new Scanner(System.in);
                        System.out.println("Please enter database password: ");
                        dbPassword = in.nextLine();
                        System.out.println("Please confirm database password: ");
                        dbPasswordConfirm = in.nextLine();
                    } else {
                        dbPassword = new String(System.console().readPassword("Please enter database password: "));
                        dbPasswordConfirm = new String(System.console().readPassword("Please confirm database password: "));
                    }
                    if (!dbPassword.equals(dbPasswordConfirm)) {
                        System.out.println("Passwords do not match");
                    }
                }
            }
            //set password
            if(StringUtils.isNotEmpty(dbPassword)) {
                AppConfig.encryptProperty("dbPassword", dbPassword);
                //if password not set generate a random
            } else {
                System.out.println("Generating random database password");
                AppConfig.encryptProperty("dbPassword", RandomStringUtils.random(32, true, true));
            }
            //else encrypt password if plain-text
        } else if (!AppConfig.isPropertyEncrypted("dbPassword")) {
            AppConfig.encryptProperty("dbPassword", AppConfig.getProperty("dbPassword"));
        }

        try {
            connection = DBUtils.getConn();
            statement = connection.createStatement();

            ResultSet rs = statement.executeQuery("select * from information_schema.tables where upper(table_name) = 'USERS' and table_schema='PUBLIC'");
            if (rs == null || !rs.next()) {
                statement.executeUpdate("create table if not exists users (id INTEGER PRIMARY KEY AUTO_INCREMENT, first_nm varchar, last_nm varchar, email varchar, username varchar not null unique, password varchar, auth_token varchar, auth_type varchar not null default '" + Auth.AUTH_BASIC+ "', user_type varchar not null default '" + Auth.ADMINISTRATOR + "', salt varchar, otp_secret varchar)");
                statement.executeUpdate("create table if not exists user_theme (user_id INTEGER PRIMARY KEY, bg varchar(7), fg varchar(7), d1 varchar(7), d2 varchar(7), d3 varchar(7), d4 varchar(7), d5 varchar(7), d6 varchar(7), d7 varchar(7), d8 varchar(7), b1 varchar(7), b2 varchar(7), b3 varchar(7), b4 varchar(7), b5 varchar(7), b6 varchar(7), b7 varchar(7), b8 varchar(7), foreign key (user_id) references users(id) on delete cascade) ");
                statement.executeUpdate("create table if not exists aws_credentials (id INTEGER PRIMARY KEY AUTO_INCREMENT, access_key varchar not null, secret_key varchar not null)");
                statement.executeUpdate("create table if not exists ec2_keys (id INTEGER PRIMARY KEY AUTO_INCREMENT, key_nm varchar not null, ec2_region varchar not null, private_key varchar not null, aws_cred_id INTEGER, foreign key (aws_cred_id) references aws_credentials(id) on delete cascade)");
                statement.executeUpdate("create table if not exists system (id INTEGER PRIMARY KEY AUTO_INCREMENT, display_nm varchar, instance_id varchar not null, user varchar not null, host varchar, port INTEGER not null, key_id INTEGER, region varchar not null, state varchar, instance_status varchar, system_status varchar, m_alarm INTEGER default 0, m_insufficient_data INTEGER default 0, m_ok INTEGER default 0, foreign key (key_id) references ec2_keys(id) on delete cascade)");
                statement.executeUpdate("create table if not exists profiles (id INTEGER PRIMARY KEY AUTO_INCREMENT, nm varchar not null, tag varchar not null)");
                statement.executeUpdate("create table if not exists user_map (user_id INTEGER, profile_id INTEGER, foreign key (user_id) references users(id) on delete cascade, foreign key (profile_id) references profiles(id) on delete cascade, primary key (user_id, profile_id))");

                statement.executeUpdate("create table if not exists status (id INTEGER, user_id INTEGER, status_cd varchar not null default 'INITIAL', foreign key (id) references system(id) on delete cascade, foreign key (user_id) references users(id) on delete cascade)");
                statement.executeUpdate("create table if not exists scripts (id INTEGER PRIMARY KEY AUTO_INCREMENT, user_id INTEGER, display_nm varchar not null, script varchar not null, foreign key (user_id) references users(id) on delete cascade)");

                statement.executeUpdate("create table if not exists session_log (id BIGINT PRIMARY KEY AUTO_INCREMENT, session_tm timestamp default CURRENT_TIMESTAMP, first_nm varchar, last_nm varchar, username varchar not null, ip_address varchar)");
                statement.executeUpdate("create table if not exists terminal_log (session_id BIGINT, instance_id INTEGER, output varchar not null, log_tm timestamp default CURRENT_TIMESTAMP, display_nm varchar not null, user varchar not null, host varchar not null, port INTEGER not null, foreign key (session_id) references session_log(id) on delete cascade)");

                //if exists readfile to set default password
                String salt = EncryptionUtil.generateSalt();
                String defaultPassword = EncryptionUtil.hash("changeme" + salt);
                File file = new File("/opt/ec2box/instance_id");
                if (file.exists()) {
                    String str = FileUtils.readFileToString(file, "UTF-8");
                    if(StringUtils.isNotEmpty(str)) {
                        defaultPassword = EncryptionUtil.hash(str.trim() + salt);
                    }
                }

                //insert default admin user
                statement.executeUpdate("insert into users (username, password, user_type, salt) values('admin', '" + defaultPassword + "','"+ Auth.MANAGER+"','"+ salt+"')");

            }

            DBUtils.closeRs(rs);
            

        } catch (Exception ex) {
            log.error(ex.toString(), ex);
        }
        finally {
            DBUtils.closeStmt(statement);
            DBUtils.closeConn(connection);
        }
        

    }

}
