/**
 * Copyright (C) 2018 Loophole, LLC
 * <p>
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * As a special exception, the copyright holders give permission to link the
 * code of portions of this program with the OpenSSL library under certain
 * conditions as described in each individual source file and distribute
 * linked combinations including the program with the OpenSSL library. You
 * must comply with the GNU Affero General Public License in all respects for
 * all of the code used other than as permitted herein. If you modify file(s)
 * with this exception, you may extend this exception to your version of the
 * file(s), but you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version. If you delete this
 * exception statement from all source files in the program, then also delete
 * it in the license file.
 */
package io.bastillion.manage.db;

import io.bastillion.manage.util.DBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * DAO to set default region
 */
public class DefaultRegionDB {

    private static Logger log = LoggerFactory.getLogger(DefaultRegionDB.class);


    private DefaultRegionDB() {
    }


    /**
     * returns default region
     *
     * @return region default region
     */
    public static String getRegion() {


        String region = null;

        Connection con = null;
        try {
            con = DBUtils.getConn();

            PreparedStatement stmt = con.prepareStatement("select * from default_region");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                region = rs.getString("region");
            }
            DBUtils.closeRs(rs);
            DBUtils.closeStmt(stmt);


        } catch (Exception ex) {
            log.error(ex.toString(), ex);
        } finally {
            DBUtils.closeConn(con);
        }

        return region;

    }

    /**
     * updates default region
     *
     * @param region default region
     */
    public static void updateRegion(String region) {

        //get db connection
        Connection con = DBUtils.getConn();

        try {
            //update
            PreparedStatement stmt = con.prepareStatement("update default_region set region = ?");
            stmt.setString(1, region);
            stmt.execute();

            DBUtils.closeStmt(stmt);

        } catch (Exception e) {
            log.error(e.toString(), e);
        } finally {
            DBUtils.closeConn(con);
        }

    }

    /**
     * inserts default region
     *
     * @param region default region
     */
    public static void insertRegion(String region) {

        //get db connection
        Connection con = DBUtils.getConn();

        try {
            //insert
            PreparedStatement stmt = con.prepareStatement("insert into default_region (region) values(?)");
            stmt.setString(1, region);
            stmt.execute();

            DBUtils.closeStmt(stmt);

        } catch (Exception e) {
            log.error(e.toString(), e);
        } finally {
            DBUtils.closeConn(con);
        }
    }

    /**
     * insert or updated an region
     *
     * @param region default region
     */
    public static void saveRegion(String region) {

        String regionTmp = getRegion();
        if (regionTmp != null) {
            updateRegion(region);
        } else {
            insertRegion(region);
        }

    }

}
