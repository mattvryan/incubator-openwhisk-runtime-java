/*
 * Copyright 2015-2016 IBM Corporation
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

package common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Properties that describe a whisk installation
 */
public class WhiskProperties {

    /**
     * The name of the properties file.
     */
    protected static final String WHISK_PROPS_FILE = "whisk.properties";

    /**
     * The root of the whisk installation.
     */
    private static final String whiskHome = getWhiskHome();

    /**
     * Default concurrency level if otherwise unspecified
     */
    private static final int DEFAULT_CONCURRENCY = 20;

    /**
     * The deployment target, e.g., local.
     */
    public static final String deployTarget = System.getProperty("deploy.target");
    
    /**
     * If true, then tests will direct to the router rather than the edge components.
     */
    public static final boolean testRouter = System.getProperty("test.router", "false").equals("true");

    /**
     * The number of tests to run concurrently.
     */
    public static final int concurrentTestCount = getConcurrentTestCount(System.getProperty("testthreads", null));
    
    /**
     * The properties read from the WHISK_PROPS_FILE.
     */
    private static final Properties whiskProperties = loadProperties(getFileRelativeToWhiskHome(WHISK_PROPS_FILE));

    static {
        System.out.format("deploy target %s\n", deployTarget != null ? deployTarget : "not defined");
        System.out.format("test router? %s\n", testRouter);
        //System.out.println(WhiskProperties.whiskProperties);
    }

    public static String getProperty(String string) {
        return whiskProperties.getProperty(string);
    }

    public static File getFileRelativeToWhiskHome(String name) {
        return new File(whiskHome, name);
    }

    public static String getKafkaHost() {
        return whiskProperties.getProperty("kafka.host");
    }

    public static int getKafkaPort() {
        return Integer.parseInt(whiskProperties.getProperty("kafka.host.port"));
    }

    public static int getKafkaMonitorPort() {
        return Integer.parseInt(whiskProperties.getProperty("kafkaras.host.port"));
    }
   

    public static String getConsulServerHost() {
        return whiskProperties.getProperty("consulserver.host");
    }

    public static int getConsulKVPort() {
        return Integer.parseInt(whiskProperties.getProperty("consul.host.port4"));
    }

    public static String getZookeeperHost() {
        return whiskProperties.getProperty("zookeeper.host");
    }

    public static int getZookeeperPort() {
        return Integer.parseInt(whiskProperties.getProperty("zookeeper.host.port"));
    }

    public static String getMainDockerEndpoint() {
        return whiskProperties.getProperty("main.docker.endpoint");
    }

    public static String getKafkaDockerEndpoint() {
        return whiskProperties.getProperty("kafka.docker.endpoint");
    }

    
    public static boolean useCliDownload() {
        return whiskProperties.getProperty("use.cli.download").equals("true");
    }

    public static String[] getInvokerHosts() {
        // split of empty string is non-empty array
        String hosts = whiskProperties.getProperty("invoker.hosts");
        return (hosts == null || hosts.equals("")) ? new String[0] : hosts.split(","); 
    }

    public static String[] getAdditionalHosts() {
        // split of empty string is non-empty array
        String hosts = whiskProperties.getProperty("additional.hosts");
        return (hosts == null || hosts.equals("")) ? new String[0] : hosts.split(",");
    }

    public static int numberOfInvokers() {
        return getInvokerHosts().length;
    }

    public static String getSslCertificateChallenge() {
        return whiskProperties.getProperty("whisk.ssl.challenge");
    }

    /**
     * Note that when testRouter == true, we pretend the router host is the edge host!
     */
    public static String getEdgeHost() {
        return testRouter ? getRouterHost() :  whiskProperties.getProperty("edge.host");
    }
    
    public static String getRealEdgeHost() {
        return whiskProperties.getProperty("edge.host");
    }
    
    public static String getAuthForTesting() {
        return whiskProperties.getProperty("testing.auth");
    }
    
    public static String getRouterHost() {
        return whiskProperties.getProperty("router.host");
    }

    public static int getEdgeHostApiPort() {
        return Integer.parseInt(whiskProperties.getProperty("edge.host.apiport"));
    }

    public static String getLoadbalancerHost() {
        return whiskProperties.getProperty("loadbalancer.host");
    }

    public static int getLoadbalancerPort() {
        return Integer.parseInt(whiskProperties.getProperty("loadbalancer.host.port"));
    }
    
    /**
     * are we running on Mac OS X?
     */
    public static boolean onMacOSX() {
        String osname = System.getProperty("os.name");
        return osname.toLowerCase().contains("mac");
    }

    /**
     * are we running on Linux?
     */
    public static boolean onLinux() {
        String osname = System.getProperty("os.name");
        return osname.equalsIgnoreCase("linux");
    }

    /**
     * where is python 2.7?
     */
    public static final String python = findPython();

    /**
     * Finds the whisk home directory. This is a path in the directory tree up
     * to the current directory that contains the file WHISK_PROPS_FILE.
     *
     * @return the path to whisk home as a string
     */
    private static String getWhiskHome() {
        String dir = System.getProperty("user.dir");

        if (dir != null) {
            // Look in the directory tree recursively.
            File propfile = findFileRecursively(dir, WHISK_PROPS_FILE);
            if (propfile != null) {
                // adjust path for blue vs open
                String parent = propfile.getParent();
                File open = new File(parent, "../open");
                if (open.isDirectory() && open.exists())
                    return open.getAbsolutePath();
                else return parent;
            } else return null;
        } else return null;
    }

    private static File findFileRecursively(String dir, String needle) {
        if (dir != null) {
            File base = new File(dir);
            File file = new File(base, needle);
            if (file.exists()) {
                return file;
            } else {
                return findFileRecursively(base.getParent(), needle);
            }
        } else return null;
    }

    /**
     * Load properties from whisk.properties
     */
    protected static Properties loadProperties(File propsFile) {
        Properties props = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(propsFile);
            // load a properties file
            props.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return props;
    }

    private static String findPython() {
        File p27 = new File("/usr/local/bin/python2.7");
        if (p27.exists()) {
            return "/usr/local/bin/python2.7";
        } else {
            return "python";
        }
    }

    private static int getConcurrentTestCount(String count) {
        if (count != null && count.trim().isEmpty() == false) {
            try {
                int threads = Integer.parseInt(count);
                if (threads > 0) {
                    return threads;
                }
            } catch (NumberFormatException e) {}
        }
        return DEFAULT_CONCURRENCY;
    }

    /**
     * read the contents of auth key file and return as a Pair
     * <username,password>
     */
    public static Pair<String, String> getBasicAuth() {
        File f = getAuthFileForTesting();
        String contents = readAuthKey(f);
        String[] parts = contents.split(":");
        assert parts.length == 2;
        return Pair.make(parts[0], parts[1]);
    }

    /**
     * @return the path to a file holding the auth key used during junit testing
     */
    static File getAuthFileForTesting() {
        String testAuth = getAuthForTesting();
        String relativePath = testAuth.equals("guest") ? "config/keys/auth.guest" : "../credentials/" + testAuth;
        File f = WhiskProperties.getFileRelativeToWhiskHome(relativePath);
        return f;
    }

    /**
     * read the contents of a file which holds an auth key.
     */
    public static String readAuthKey(File filename) {
        // the following funny relative path works both from Eclipse and when
        // running in bin/ directory from ant
        try {
            byte[] encoded = Files.readAllBytes(filename.toPath());
            String authKey = new String(encoded, "UTF-8").trim();
            return authKey;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
