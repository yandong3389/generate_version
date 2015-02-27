package com.clbus.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * 类名称：GenerateVersionUtils
 * 内容摘要：追加JSP文件中引用JS及CSS文件的SVN版本号
 * @author 闫冬
 * @version 1.0 2012-6-7
 */
public class GenerateVersionUtils {

    /** SVN地址 */
    private static String url = "http://192.168.8.199/svn/svn";
    /** SVN用户名称 */
    private static String username = "yand";
    /** SVN用户密码 */
    private static String password = "123456";
    private static SVNRepository repository;
    private static int jspFileWriteCount = 0;
    private static int cssjsFileCount = 0;
    static SimpleDateFormat format = new SimpleDateFormat();

    static {

        // 时间格式
        format.applyPattern("yyyy-MM-dd HH:mm:ss");
        // 初始化
        DAVRepositoryFactory.setup();
        // 提供认证
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
        try {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
            repository.setAuthenticationManager(authManager);
        } catch (SVNException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据路径循环取得SVN上的JS、CSS文件的版本号,可按最后提交时候来生成版本号
     * @param path JS、CSS文件的目录路径
     * @param generateJspPath JSP文件的路径
     * @param startDate 最后提交时间的起始时间
     * @param endDate 最后提交时间的截止时间
     * @throws Exception
     * @author 闫冬
     */
    @SuppressWarnings("unchecked")
    public static void eachSvnFilesGenerateVersion(String path, String generateJspPath, Date startDate, Date endDate)
            throws Exception {

        path = path.replace("\\", "/");
        path = path.matches("^/(\\s|\\S){1,}/$") ? path : "/" + path + "/";

        // 取得指定路径下的文件列表
        Collection<SVNDirEntry> entries = repository.getDir(path, -1, null, (Collection<?>) null);

        // 遍历从SVN上取得的文件列表
        for (SVNDirEntry entry : entries) {
            if (entry.getKind() == SVNNodeKind.FILE) {

                // 如果根据CSS、JS的最后提交时间来生成版本号
                if (startDate != null && endDate != null) {

                    // 判断当前文件是否在指定的时间段内,如果是则生成,不是则跳过此文件
                    if (entry.getDate().getTime() > startDate.getTime()
                            && entry.getDate().getTime() < endDate.getTime()) {

                        // 过滤出CSS和JS文件
                        if (entry.getName().endsWith(".css") || entry.getName().endsWith(".js")) {

                            cssjsFileCount++;
                            // 取得最后提交的版本号
                            long lastCommitRevision = entry.getRevision();
                            // 取得文件的全路径
                            String fileName = entry.getURL().toDecodedString();
                            System.out.println("版本号：" + lastCommitRevision + "   文件名称：" + fileName);
                            // 取得JSP导入CSS、JS的路径
                            fileName = fileName.substring(fileName.lastIndexOf("web/"), fileName.length());
                            // 生成JSP中JS、CSS的对应的版本号
                            generateVersion(new File(generateJspPath), fileName, String.valueOf(lastCommitRevision));
                        }
                    }
                }
                // 生成全部CSS、JS文件的版本号
                else {

                    // 过滤出CSS和JS文件
                    if (entry.getName().endsWith(".css") || entry.getName().endsWith(".js")) {

                        cssjsFileCount++;
                        // 取得最后提交的版本号
                        long lastCommitRevision = entry.getRevision();
                        // 取得文件的全路径
                        String fileName = entry.getURL().toDecodedString();
                        System.out.println("版本号：" + lastCommitRevision + "   文件名称：" + fileName);
                        // 取得JSP导入CSS、JS的路径
                        fileName = fileName.substring(fileName.lastIndexOf("web/"), fileName.length());
                        generateVersion(new File(generateJspPath), fileName, String.valueOf(lastCommitRevision));
                    }
                }
            }
            else {
                // 如果是文件夹, 则递归调用生成版本号方法
                eachSvnFilesGenerateVersion(path + entry.getName(), generateJspPath, startDate, endDate);
            }
        }
    }

    /**
     * 在JSP文件中添加JS及CSS文件的版本号
     * @param file JSP文件的路径或路径下文件
     * @param jsFileName JS或CSS文件名称
     * @param revision JS或CSS文件的SVN修订版本号
     * @author 闫冬
     */
    public static void generateVersion(File file, String jsFileName, String revision) {

        try {
            StringBuffer strBuf = new StringBuffer();

            // 如果是文件夹,过滤路径：src、work、help、info、classes、web、news、lib
            // 只遍历根目录下的.jsp和WEB-INF/jsp目录下的.jsp文件
            if (file.isDirectory() && file.getPath().indexOf("src") == -1 && file.getPath().indexOf("\\work\\") == -1
                    && file.getPath().indexOf("help") == -1 && file.getPath().indexOf("info") == -1
                    && file.getPath().indexOf("classes") == -1 && file.getPath().indexOf("\\web\\") == -1
                    && file.getPath().indexOf("news") == -1 && file.getPath().indexOf("lib") == -1) {

                // 过滤掉隐藏的文件(.svn文件夹)
                File[] files = file.listFiles(new FileFilter() {

                    public boolean accept(File pathname) {
                        // 过滤隐藏文件
                        return !pathname.isHidden();
                    }
                });

                // 遍历文件夹下的所有文件
                if (files != null && files.length > 0) {
                    for (int i = 0; i < files.length; i++) {
                        generateVersion(files[i], jsFileName, revision);
                    }
                }
            }
            // 如果是JSP文件,刚修改文件加入版本号
            else if (file.getName().endsWith(".jsp")) {
                jspFileWriteCount++;
                BufferedReader bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                // 定义行号
                int lineNumber = 0;
                for (String tmp = null; (tmp = bufReader.readLine()) != null; tmp = null) {

                    // 除第一行内容都追加换行符
                    if (lineNumber > 0) {
                        strBuf.append(System.getProperty("line.separator"));
                    }
                    lineNumber++;

                    // 如果该JSP已生成了JS对应的版本号,刚去掉版本号重新再生成
                    if (tmp.indexOf("?revision=") > -1 && tmp.indexOf(jsFileName) > -1) {
                        // 替换成新的版本号
                        tmp = tmp.replaceAll("(\\?revision=[0-9]{1,})", "?revision=" + revision);
                    }
                    else {
                        // 在这里做替换操作,新增版本号
                        tmp = tmp.replaceAll(jsFileName, jsFileName + "?revision=" + revision);
                    }
                    strBuf.append(tmp);
                }
                bufReader.close();
                PrintWriter printWriter = new PrintWriter(file);
                printWriter.write(strBuf.toString().toCharArray());
                strBuf.setLength(0);
                printWriter.flush();
                printWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 主函数,执行生成js及css文件版本号的主方法,在此方法中指定css、js的路径以及要生成版本号的jsp路径
     * @param args
     * @author 闫冬
     */
    public static void main(String args[]) {

    	// TODO 如下参数需修改
    	
        try {

            // 项目的SVN路径(用于取得CSS、JS的SVN版本号的路径)
            String svnJsPath = "08_制造工程\\01_所有程序\\jrb";

            // 要生成版本号的JSP文件路径,可直接写sp_web的路径
            String jspPath = "E:\\workspace\\jrb\\src\\main\\webapp\\";

            // 根据更新时间取得CSS及JS文件(起始时间-截止时间)默认当前时间向前两个月内
            Date endDate = new Date();
            Calendar ca = Calendar.getInstance();
            ca.setTime(endDate);
            ca.add(2, -1);
            Date startDate = ca.getTime();

            Date date = new Date();
            eachSvnFilesGenerateVersion(svnJsPath + "\\resources\\js", jspPath, startDate, endDate);
            System.out.println("js文件版本生成完毕！用时：" + (new Date().getTime() - date.getTime()) / 1000 + "秒");

            Date date2 = new Date();
            eachSvnFilesGenerateVersion(svnJsPath + "\\resources\\css", jspPath, startDate, endDate);
            System.out.println("css文件版本生成完毕！用时：" + (new Date().getTime() - date2.getTime()) / 1000 + "秒");

            System.out.println("js及css文件个数：" + cssjsFileCount);
            if (cssjsFileCount > 0) {
                System.out.println("jsp文件个数：" + jspFileWriteCount / cssjsFileCount);
                System.out.println("jsp共读写次数：" + jspFileWriteCount);
            }
            System.out.println("总共用时：" + (new Date().getTime() - date.getTime()) / 1000 + "秒");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}