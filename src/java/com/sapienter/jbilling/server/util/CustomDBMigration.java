package com.sapienter.jbilling.server.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * Creating a custom change class because we need to add auto increment primary
 * keys to tables that already have data. So we need to fill up this column with
 * sequential data before we can set it up to a sequence
 * 
 * @author maruthi
 * 
 */
public class CustomDBMigration implements CustomTaskChange {

	@Override
	public String getConfirmationMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFileOpener(ResourceAccessor arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setUp() throws SetupException {
		// TODO Auto-generated method stub

	}

	@Override
	public ValidationErrors validate(Database arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void execute(Database db) throws CustomChangeException {
		try {
			JdbcConnection con = (JdbcConnection) db.getConnection();
			Statement st = con.createStatement(ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_UPDATABLE,
					ResultSet.HOLD_CURSORS_OVER_COMMIT);
			
			Statement st2 = con.createStatement(ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_UPDATABLE,
					ResultSet.HOLD_CURSORS_OVER_COMMIT);

/*			st.execute("DROP SEQUENCE IF EXISTS filter_set_filter_id_seq");
			st.execute("DROP SEQUENCE IF EXISTS user_credit_card_map_id_seq");
			st.execute("DROP SEQUENCE IF EXISTS list_meta_field_values_id_seq");*/

			ResultSet rs = st.executeQuery("select * from filter_set_filter");
			int counter = 1;
			while (rs.next()) {
				st2.executeUpdate("update filter_set_filter set id=" + counter
						+ " where filter_set_filters_id="
						+ rs.getInt("filter_set_filters_id")
						+ " and filter_id=" + rs.getInt("filter_id"));
				counter++;
			}
			
			st.execute("CREATE SEQUENCE filter_set_filter_id_seq START " + counter);

			rs = st.executeQuery("select * from user_credit_card_map");
			counter = 1;
			while (rs.next()) {
				st2.executeUpdate("update user_credit_card_map set id="
						+ counter + " where user_id=" + rs.getInt("user_id")
						+ " and credit_card_id=" + rs.getInt("credit_card_id"));
				counter++;
			}
			
			
			st.execute("CREATE SEQUENCE user_credit_card_map_id_seq START " + counter);

			rs = st.executeQuery("select * from list_meta_field_values");
			counter = 1;
			while (rs.next()) {
				st2.executeUpdate("update list_meta_field_values set id="
						+ counter + " where meta_field_value_id="
						+ rs.getInt("meta_field_value_id") + " and list_value="
						+ rs.getInt("list_value"));
				counter++;
			}
			
			st.execute("CREATE SEQUENCE list_meta_field_values_id_seq START " + counter);

			st.execute("ALTER TABLE filter_set_filter ADD PRIMARY KEY (id)");
			st.execute("ALTER TABLE user_credit_card_map ADD PRIMARY KEY (id)");
			st.execute("ALTER TABLE list_meta_field_values ADD PRIMARY KEY (id)");
			st.execute("ALTER TABLE filter_set_filter ALTER COLUMN id SET DEFAULT nextval('filter_set_filter_id_seq')");
			st.execute("ALTER TABLE user_credit_card_map ALTER COLUMN id SET DEFAULT nextval('user_credit_card_map_id_seq')");
			st.execute("ALTER TABLE list_meta_field_values ALTER COLUMN id SET DEFAULT nextval('list_meta_field_values_id_seq')");
		} catch (DatabaseException e) {
			e.printStackTrace();
			throw new CustomChangeException(e);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CustomChangeException(e);
		}
	}

}
