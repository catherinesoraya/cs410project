import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;
import java.util.StringTokenizer;

import com.jcraft.jsch.*;


public class TaskDestroyer {

	private static Connection con;

	public static void main(String[] args) throws SQLException, ClassNotFoundException, JSchException {

		if(args.length < 5 || args.length > 5) {
			System.out.println("Usage: java TaskDestroyer <BroncoUserid> <BroncoPassword> <sandboxUSerID> <sandbox password> <yourportnumber>");
			System.exit(0);
		}

		Connection con = null;
		Session session = null;
		Statement stmt = null, stmt2 = null;
		try
		{
			String strSshUser = args[0];                  // SSH loging username
			String strSshPassword = args[1];                   // SSH login password
			String strSshHost = "onyx.boisestate.edu";          // hostname or ip or SSH server
			int nSshPort = 22;                                    // remote SSH host port number
			String strRemoteHost = "localhost";  // hostname or ip of your database server
			int nLocalPort = 3367;  // local port number use to bind SSH tunnel

			String strDbUser = args[2];                    // database loging username
			String strDbPassword = args[3];                    // database login password
			int nRemotePort = Integer.parseInt(args[4]); // remote port number of your database 

			/*
			 * STEP 0
			 * CREATE a SSH session to ONYX
			 * 
			 * */
			session = TaskDestroyer.doSshTunnel(strSshUser, strSshPassword, strSshHost, nSshPort, strRemoteHost, nLocalPort, nRemotePort);


			/*
			 * STEP 1 and 2
			 * LOAD the Database DRIVER and obtain a CONNECTION
			 * 
			 * */
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://localhost:"+nLocalPort, strDbUser, strDbPassword);
			con.setAutoCommit(false);

			System.out.println("Connection successful!\n\n");


			ScanMan();





		}
		catch( SQLException e )
		{
			System.out.println(e.getMessage());
			con.rollback(); // In case of any exception, we roll back to the database state we had before starting this transaction
		}
		finally{

			/*
			 * STEP 5
			 * CLOSE CONNECTION AND SSH SESSION
			 * 
			 * */

			if(stmt!=null)
				stmt.close();

			if(stmt2!=null)
				stmt.close();

			con.setAutoCommit(true); // restore dafault mode
			con.close();
			session.disconnect();
		}
	}	

	private static void ScanMan() throws SQLException {

		System.out.println("Welcome to the Task Destroyer.");



		Scanner scan = new Scanner(System.in);



		String response = scan.nextLine();

		while(!response.equals("exit")){

			HandleResponse(response);

			con.commit(); //transaction block ends

			System.out.println("Transaction done!");

			response = scan.nextLine();

		}



	}

	private static void HandleResponse(String response) {

		Statement stmt;

		String SqlStmt = "";
		ResultSet rs;

		StringTokenizer tok = new StringTokenizer(response, ",");


		while(tok.hasMoreTokens()) {
			String command = tok.nextToken();
			StringTokenizer tok2 = new StringTokenizer(command, " ");

			try {
				stmt = con.createStatement();
				if(command.startsWith("active")) {
					tok2.nextToken();
					String tag = tok2.nextToken();
					if(tag == null) {
						SqlStmt = "SELECT * FROM Task WHERE status = 'active'";
					}else {
						SqlStmt = "SELECT * FROM Task WHERE status = 'active' AND tag LIKE '%"+tag+"%'";		
					}
					rs = stmt.executeQuery(SqlStmt);
					System.out.println(PrintResults(rs));

				}else if(command.startsWith("add ")) {
					tok2.nextToken();
					String taskLabel = "";
					while(tok2.hasMoreTokens()) {
						taskLabel += tok2.nextToken() + " ";
					}
					if(taskLabel.isEmpty()) {
						System.out.println("Usage: add <task label>");
						break;
					}

					SqlStmt = "INSERT INTO Task ....";
					stmt.execute(SqlStmt);

					SqlStmt = "SELECT taskID FROM Task WHERE label = '"+taskLabel+"'";
					rs = stmt.executeQuery(SqlStmt);
					System.out.println("task ID: "+ PrintResults(rs));

				} else if(command.startsWith("due ")) {
					tok2.nextToken();
					String next = tok2.nextToken();
					if(next != null) {

						if(next.equals("soon")) {
							SqlStmt = "SELECT * FROM Task WHERE dueDate <= ?";
							rs = stmt.executeQuery(SqlStmt);
							System.out.println(PrintResults(rs));
						} else if (next.equals("today")) {
							SqlStmt = "SELECT * FROM Task WHERE dueDate = GETDATE()";
							rs = stmt.executeQuery(SqlStmt);
							System.out.println(PrintResults(rs));
						} else {

							try {
								int taskId = Integer.parseInt(next);
								String date = tok2.nextToken();
								SqlStmt = "UPDATE Task SET dueDate = "+date+" WHERE taskId = "+taskId;
								stmt.executeUpdate(SqlStmt);
							} catch (NumberFormatException nfe) {
								System.out.println("Usage: due soon | due today | due <task id> <date>");
							}
						}
					}
				}else if(command.startsWith("tag")) {
					tok2.nextToken();
					String Id = tok2.nextToken();
					String next = tok2.nextToken();
					
					SqlStmt = "SELECT tag FROM Task WHERE taskId = "+ Id;
					
					rs = stmt.executeQuery(SqlStmt);
					
					String oldTag = PrintResults(rs);
					
					
					if(oldTag != null)
						SqlStmt = "UPDATE Task SET tag = '" + oldTag ;
					else
						SqlStmt = "UPDATE Task SET tag = '";
					
					
					while(next != null) {
						
						SqlStmt += " " + next ;
						next = tok2.nextToken();
						
					}
					SqlStmt += "' WHERE taskId = " + Id;
					stmt.executeUpdate(SqlStmt);
					
				}else if(command.startsWith("finish")) {
					tok2.nextToken();
					String Id = tok2.nextToken();
					
					SqlStmt = "UPDATE Task SET status = 'completed' WHERE taskId = " + Id;
					stmt.execute(SqlStmt);
					
				}else if(command.startsWith("cancel")) {
					tok2.nextToken();
					String Id = tok2.nextToken();
					
					SqlStmt = "UPDATE Task SET status = 'canceled' WHERE taskId = " + Id;
					stmt.execute(SqlStmt);
					
				}else if(command.startsWith("show")) {
					tok2.nextToken();
					String Id = tok2.nextToken();
					SqlStmt = "SELECT * FROM Task WHERE taskId = " + Id;
					
					rs = stmt.executeQuery(SqlStmt);
					
					System.out.println(PrintResults(rs));
				}else if(command.startsWith("completed")) {
					tok2.nextToken();
					String tag = tok2.nextToken();
					SqlStmt = "SELECT taskId, label FROM Task WHERE status = 'completed' AND tag LIKE '%"+tag+"%'";		
					rs = stmt.executeQuery(SqlStmt);
					System.out.println(PrintResults(rs));
					
				}
				


			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		}
	}

	private static String PrintResults(ResultSet resultSet) throws SQLException{
		ResultSetMetaData rsmd = resultSet.getMetaData();
		String out = "";
		int columnsNumber = rsmd.getColumnCount();
		while (resultSet.next()) {
			for (int i = 1; i <= columnsNumber; i++) {
				if (i > 1) out+=",  ";
				String columnValue = resultSet.getString(i);
				out += columnValue + " " + rsmd.getColumnName(i);
			}
			out += " ";
		}
		return out;
	}

	private static Session doSshTunnel( String strSshUser, String strSshPassword, String strSshHost, int nSshPort, String strRemoteHost, int nLocalPort, int nRemotePort ) throws JSchException
	{
		/*This is one of the available choices to connect to mysql
		 * If you think you know another way, you can go ahead*/

		final JSch jsch = new JSch();
		java.util.Properties configuration = new java.util.Properties();
		configuration.put("StrictHostKeyChecking", "no");

		Session session = jsch.getSession( strSshUser, strSshHost, 22 );
		session.setPassword( strSshPassword );

		session.setConfig(configuration);
		session.connect();
		session.setPortForwardingL(nLocalPort, strRemoteHost, nRemotePort);
		return session;
	}

}