import java.io.*;	
public class Request {
		private String task;
		private DataOutputStream outToClient;
		public Request(String t, DataOutputStream out) {
			task = t;
			outToClient = out;
		}
		public String getTask() {
			return this.task;
		}
		public DataOutputStream getClientContact() {
			return this.outToClient;
		}
}
