import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

class DATA{
    String Path;
    String Subject;
    String Author;
    String Addressee;
    String Content;
    DATA()
    {
        this.Path      = "";
        this.Subject   = "";
        this.Author    = "";
        this.Addressee = "";
        this.Content   = "";
    }
}

public class SaveDataToMysql {
    // MySQL - JDBC驱动名及数据库、URL、用户、密码
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "root";

    // 获取path下所有文件夹
    private static List<File> getFiles(String path) {
        File root = new File(path);
        List<File> files = new ArrayList<File>();
        if (!root.isDirectory()) {
            files.add(root);
        } else {
            File[] subFiles = root.listFiles();
            assert subFiles != null;
            for (File f : subFiles) {
                files.addAll(getFiles(f.getAbsolutePath()));
            }
        }
        return files;
    }
    // 切割各个文件获取关键信息
    private static List<DATA> getData(List<File> files) {
        List<DATA> all_data = new ArrayList<DATA>();
        for (File cur_file : files) { // 遍历所有文件
            String cur_file_path = cur_file.getAbsolutePath();
            try {
                DATA temp_data = new DATA();
                temp_data.Path = cur_file_path;
                FileReader f = new FileReader(cur_file_path);
                BufferedReader buf = new BufferedReader(f);
                String s;
                boolean if_content = false; // 当前是否是正文部分
                StringBuilder temp_content = new StringBuilder(); // 正文部分记录
                while ((s = buf.readLine()) != null) {
                    if (!if_content) { // 当前还没当正文部分
                        if (s.contains(":")) { // 如果当前行中包含了冒号
                            String[] tempString = s.split(":"); //按冒号切割
                            if (tempString.length >= 2) { // 切割后至少有2个部分
                                switch (tempString[0]) {
                                    case "Subject":
                                        StringBuilder temp_subject = new StringBuilder();
                                        for (int i = 1; i < tempString.length; ++i) {
                                            temp_subject.append(tempString[i]);
                                        }
                                        temp_data.Subject = temp_subject.toString();
                                        break;
                                    case "From":
                                        StringBuilder temp_from = new StringBuilder();
                                        for (int i = 1; i < tempString.length; ++i) {
                                            temp_from.append(tempString[i]);
                                        }
                                        temp_data.Author = temp_from.toString();
                                        break;
                                    case "To":
                                        StringBuilder temp_to = new StringBuilder();
                                        for (int i = 1; i < tempString.length; ++i) {
                                            temp_to.append(tempString[i]);
                                        }
                                        temp_data.Addressee = temp_to.toString();
                                        break;
                                    case "X-FileName":
                                        if_content = true; //后面开始是正文部分
                                        break;
                                    default:
                                }
                            }
                        }
                    } else { // 正文部分
                        temp_content.append(s);
                    }
                }
                temp_data.Content = temp_content.toString();
                f.close();
                buf.close();
                all_data.add(temp_data);
            } catch (IOException e) {
                System.out.println("error: " + cur_file_path);
            }
        }
        return all_data;
    }
    // 数据插入mysql中
    private static void InsertMysql (List<DATA> dates){
        Connection conn = null;
        PreparedStatement  stmt  = null;
        try {
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);
            // 打开链接
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            // 执行sql语句
            for (DATA data : dates) {
                String sql = "INSERT INTO `Email` VALUES (?,?,?,?,?)";
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, data.Path);
                stmt.setString(2, data.Subject);
                stmt.setString(3, data.Author);
                stmt.setString(4, data.Addressee);
                stmt.setString(5, data.Content);
                stmt.executeUpdate();
                System.out.println("InsertMysql: " + data.Path);
            }
            // 完成后关闭
            stmt.close();
            conn.close();
        } catch(Exception se) {
            se.printStackTrace();// 处理 JDBC 错误
        }
        finally{
                // 关闭资源
                try {
                    if (stmt != null) stmt.close();
                } catch (SQLException ignored) {
                }// 什么都不做
                try {
                    if (conn != null) conn.close();
                } catch (SQLException se) {
                    se.printStackTrace();
                }
            }
    }

    public static void main(String[] args) {
        List<File> files = getFiles("data\\maildir");
        List<DATA> dates = getData(files);
        InsertMysql(dates);
        System.out.println("end");
    }
}

