package com.example.javahomework2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private Handler uiHandler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    ShowResult((Vector<String>) msg.obj);
                    break;
                case 2:
                    Toast toast = Toast.makeText(getApplicationContext(), "检索失败", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
            }
        }
    };

    public void SearchEmail(View v) {
        RadioGroup radio_group_type = findViewById(R.id.RadioGroup);
        int selected = radio_group_type.getCheckedRadioButtonId();
        String SendInfo = "SELECT Path from email where ";
        switch (selected) {
            case R.id.radioButton_title:
                SendInfo += "Subject LIKE \"%";
                break;
            case R.id.radioButton_writer:
                SendInfo += "Author LIKE \"%";
                break;
            case R.id.radioButton_Addressee:
                SendInfo += "Addressee LIKE \"%";
                break;
            case R.id.radioButton_content:
                SendInfo += "Content LIKE \"%";
                break;
            default:
                SendInfo += "NULL";
                break;
        }
        if (SendInfo.equals("SELECT Path from Email where ")) //用户没有选择检索类型
        {
            Toast toast = Toast.makeText(getApplicationContext(), "检索失败：没有选择检索类型", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        EditText EditText_content = findViewById(R.id.editText);
        String content = EditText_content.getText().toString();
        if (content.equals("")) //用户没有输入检索内容
        {
            Toast toast = Toast.makeText(getApplicationContext(), "检索失败：没有选择检索内容", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        SendInfo += content;
        SendInfo += "%\";";
        final String final_info = SendInfo;

        new Thread(new Runnable() {
            public void run() {
                DBConnection db = new DBConnection();
                db.linkMysql(final_info);
            }
        }).start();
    }

    public void ShowResult(Vector<String> Info){
        AlertDialog.Builder ResultDialog = new AlertDialog.Builder(MainActivity.this);
        ResultDialog.setTitle("查询结果");
        String content = "";
        for(int i = 0;i < Info.size(); ++i){
            content += "["+ (i + 1) + "] " + Info.get(i) + "\n";
        }
        ResultDialog.setMessage(content);
        ResultDialog.setNegativeButton("返回", null);
        ResultDialog.show();
    }

    public class DBConnection {
        private static final String DBDRIVER = "com.mysql.jdbc.Driver";
        private static final String DBURL = "jdbc:mysql://106.52.165.12:3306/test?useUnicode=true&characterEncoding=utf-8&useSSL=false";
        private static final String DBUSER = "root";
        private static final String DBPASSWORD = "";

        public boolean linkMysql(String SendInfo_sql) {
            Connection conn = null;
            Statement stmt;
            Vector<String> SearchResult = new Vector();
            try {
                Class.forName(DBDRIVER);
            }
            catch (Exception e){
                e.printStackTrace();
                return false;
            }
            try{
                conn = DriverManager.getConnection(DBURL,DBUSER,DBPASSWORD);
                stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(SendInfo_sql);

                while (rs.next()) {
                    String temp_result1 = rs.getString("Path");
                    String temp_result2[] = temp_result1.split("maildir"+"\\\\");
                    SearchResult.add(temp_result2[1]);
                }

                rs.close();
                stmt.close();
                conn.close();

                Message msg = new Message();
                msg.what = 1;
                msg.obj = SearchResult;
                uiHandler.sendMessage(msg);

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            finally {
                if(conn!=null){
                    try {
                        conn.close();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
