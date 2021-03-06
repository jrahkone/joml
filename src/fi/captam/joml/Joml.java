package fi.captam.joml;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Joml {
	static String version = "1.4";
	public static Joml parse(String...fnames) {
		Joml j = new Joml();
		for (String fname:fnames) j._parse(fname);
		j._validate();
		return j;
	}
	Map<String,String> inherits = new LinkedHashMap<>();
	Map<String,String> map = new LinkedHashMap<>();
	Map<String,String> override = new LinkedHashMap<>();
	void _parse(String fname) {
		String str = readFile(fname);
		int linenum = 0;
		String prefix = null;
		String line = "";
		for (String l : lines(str)) {
			linenum++;
			l = trim(l);
			if (l.length()==0) continue;
			if (l.endsWith("\\")) {
				line+=l.substring(0,l.length()-1); continue;
			}
			line+=l;
			if (line.startsWith("#")) {line=""; continue;}
			line = line.replaceFirst("[^\\\\]#.*","");
			line = line.replaceAll("\\\\#", "#");
			if (line.startsWith("[")) {
				prefix = trim(line.substring(1,line.indexOf(']')));
				String p[]=prefix.split(":");
				if (p.length==2) { prefix = trim(p[0]); inherits.put(prefix,trim(p[1]));}
				line=""; continue;
			}
			int idx = line.indexOf('=');
			if (idx>0) {
				String key = trim(line.substring(0,idx));
				String value = trim(line.substring(idx+1));
				if (!empty(prefix)) key = prefix+"."+key;
				if (fname.endsWith("override.joml")) { override.put(key,value);	}
				else { map.put(key,value);}
				line="";continue;
			}
			fail("invalid line: "+linenum+" in file:"+fname+" : "+line);
		}
	}
	void _validate() { for (String key:map.keySet()) {String val = get(key);}}
	String env_name = null;
	String env_num = null;
	String env_node = null;
	void env(String name, String num, String node) { env_name = name; env_num = num; env_node = node;}
	public String get(String key) {
		if (override.containsKey(key)) return override.get(key);
		int idx = key.lastIndexOf('.'); if (idx<0) return get("",key);
		return get(key.substring(0,idx+1),key.substring(idx+1));
	}
	public String get(String prefix, String key) {
		if (eq("env-name",key)) return env_name;
		if (eq("env-num",key)) return ""+env_num;
		if (eq("env-node",key)) return ""+env_node;
		if (env_name!=null) {
			String envpr = "env."+env_name+"."+env_num;
			String prefix2 = envpr+"."+prefix;
			if (map.containsKey(prefix2+key)) { prefix=prefix2; }
			else if (inherits.containsKey(envpr)) {
				String key2 = "env."+inherits.get(envpr)+"."+prefix;
				if (map.containsKey(key2+key)) { prefix = key2;}
			}
		}
		String fk = prefix+key; String val = map.get(fk);
		if (val==null) {
			warn("WARNING: '"+fk+"' unknown in "+evalfilename+" line: "+evalfilelinenum); return null;}
		return evalLine(prefix,val);
	}

	String evalLine(String prefix, String val) {
		while (true) {
			int idx = find(val,"$(");
			if (idx < 0) break;
			String p1 = idx>0?val.substring(0,idx):"";
			String r = val.substring(idx+2);
			int idx2 = r.indexOf(')');
			if (idx2 < 0) fail("parse error:"+val);
			String ex = r.substring(0,idx2);
			String p2 = r.substring(idx2+1);
			if (ex.indexOf('.')>0) val = p1+get(ex)+p2;
			else val = p1+get(prefix,ex)+p2;
		}
		return val;
	}
	int find(String str, String part) {
		while (true) {
			int idx = str.indexOf(part);
			if (idx>0&&str.charAt(idx-1)=='\\') { str = str.substring(idx+1); continue;}
			return idx;
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String key:map.keySet()) { sb.append(key+"="+map.get(key)+"\n");}
		return sb.toString();
	}

	public int evalDir(File dir) {
		int sum = 0;
		for (File f:dir.listFiles()) {
			if (f.isDirectory()) {
				sum+=evalDir(f);
			} else {
				if (f.getName().endsWith(".tmpl")) {
					String infname = f.getAbsolutePath();
					String outfname = infname.substring(0,infname.length()-5);
					String res = evalFile(infname);
					writeFile(outfname,res);
					sum++;
				}
			}
		}
		return sum;
	}
	String evalfilename;
	int evalfilelinenum;
	public String evalFile(String fname) {
		evalfilename = fname;
		evalfilelinenum = 0;
		StringBuilder sb = new StringBuilder();
		int isif = 0;
		boolean valif[] = new boolean[10];
		for (String line:lines(readFile(fname))) {
			evalfilelinenum++;
			//print(evalfilelinenum+":"+line+" "+valif[isif]+" "+isif);
			if (line.startsWith("#if(")) {
				if (isif>0 && !valif[isif-1]) { valif[isif] = false; isif++; continue;}
				line=line.substring(4,line.indexOf(')'));
				boolean not = false;
				if (line.startsWith("!")) { not = true; line=trim(line.substring(1));}
				String p[] = line.split(",");
				String v2=trim(p[1]);
				if (Character.isDigit(v2.charAt(0))) {
					// number as string
				} else if (v2.charAt(0)=='"') {
					v2=v2.substring(1,v2.length()-1);
				} else {
					v2=get(trim(p[1]));
				}
				valif[isif] = eq(get(trim(p[0])),v2); if (not) valif[isif]=!valif[isif];
				isif++; continue;
			}
			if (line.startsWith("#end")) { isif--; continue; }
			if (isif>0 && !valif[isif-1]) {continue;}
			line = evalLine("",line);
			line = line.replace("\\$","$");
			sb.append(line+"\n");
		}
		return sb.toString();
	}

	public static void main(String args[]) {
		//run("env.joml","--env","test","--num","1","--test","conn.");
		run(args);
	}
	public static Joml run(String...args) {
		String env=null,num=null,node=null,dirname=null,dumpfname = null, testprefix=null;
		List<String> fnames = new ArrayList<>();
		for (int i=0;i<args.length;i++) {
			if (eq(args[i],"-v")) { print("joml version: "+version); return null;}
			if (eq(args[i],"--dir")) { dirname = opt(args,++i); continue;}
			if (eq(args[i],"--env")) { env = opt(args,++i); continue;}
			if (eq(args[i],"--num")) { num = opt(args,++i); continue;}
			if (eq(args[i],"--node")) { node = opt(args,++i); continue;}
			if (eq(args[i],"--dump")) { dumpfname = opt(args,++i); continue;}
			if (eq(args[i],"--test")) { testprefix = opt(args,++i); continue;}
			if (args[i].matches(".*\\.(joml|yml|jml)$")) {
				if (dumpfname!=null && args[i].contains("secret")) continue;
				fnames.add(args[i]);continue;
			}
			usage("invalid arg: "+args[i]);
		}
		if (fnames.size()==0) usage("no joml files given");
		Joml j = Joml.parse(fnames.toArray(new String[fnames.size()]));
		if (testprefix!=null) {
			j.testAll(env,num,null,testprefix);
			return j;
		}
		if (dumpfname!=null) {
			j.env(env,num,node);
			List<String> keys = new ArrayList<>(j.map.keySet());
			Collections.sort(keys);
			StringBuilder sb = new StringBuilder();
			for (String key:keys) {
				if (key.startsWith("env.")) continue;
				sb.append(key+"="+j.get(key)+"\n");
			}
			writeFile(dumpfname,sb);
			return j;
		}
		if (dirname!=null) {
			File dir = new File(dirname);
			if (!dir.isDirectory()) usage("invalid directory: "+dirname);
			j.env(env,num,node);
			j.evalDir(dir);
		}
		return j;
	}

	void testAll(String env, String num, String node, String prefix) {
		env(env,num,node);
		List<String> keys = new ArrayList<>();
		for (String key:map.keySet()) {if(key.startsWith(prefix)) keys.add(key); }
		Collections.sort(keys);
		if (prefix.startsWith("dir")) {
			print("\ntesting all directory paths:");
			print("===============================================================");
		}
		if (prefix.startsWith("conn")) {
			print("\ntesting all connections:");
			print("===============================================================");
		}
		for (String key:keys) {
			String val = get(key);
			System.out.print(String.format("%-15s = %-25s - ",key,val));
			if (empty(val)) {print("  no value"); continue;}
			if (prefix.startsWith("dir")) {
				File f = new File(val);
				if (f.exists()) {
					if (!f.isDirectory()) {
						print("file exists - not a directory"); continue;
					}
				} else {
					try {
						f.mkdirs();
					} catch (Exception e) {
						print("ERROR: could not create directory"); continue;
					}
				}
				String fname = f.getAbsolutePath()+"/ftest-"+System.currentTimeMillis()/1000;
				try {
					writeFile(fname,"test write file\n");
				} catch (Exception e) {
					print("ERROR: could not write to directory"); continue;
				}
				try {new File(fname).delete();}
				catch (Exception e) {
					print("ERROR: could not delete testfile");
				}
				print("OK");
			} else if (prefix.startsWith("conn")) {
				if (key.endsWith("-host")) {print("skip name *-host");continue;}
				if (key.endsWith("-port")) {print("skip name *-port");continue;}
				Pattern rx = Pattern.compile("(.*://)?([^:]+)(:.+)?");
				Matcher m = rx.matcher(val);
				if (m.matches()) {
					String proto = m.group(1);
					String addr = m.group(2);
					String port = m.group(3);
					int portnum = 80;
					if (port!=null) portnum = Integer.parseInt(port.substring(1));
					else {
						if (eq(proto,"http://")) portnum = 80;
						if (eq(proto,"https://")) portnum = 443;
					}
					try {
						Socket s = new Socket();
						s.connect(new InetSocketAddress(addr,portnum),1500);
						s.close();
						print("OK");
					} catch (Exception e) {
						print("ERROR: could not connect");
						continue;
					}
				} else {
					print("skipping, not connection format");
				}
			}
		}
	}

	static String opt(String args[], int idx) {
		if (idx>=args.length) usage("expected arg for "+args[idx-1]);
		return args[idx];
	}

	static void usage(String msg) {
		print("usage: java -jar joml.jar --env name --num num env.joml [other.joml...] --dir conf/");
		fail(msg);
	}


	// ====== util tree shake =======
	public static String readFile(String fname) {try {return new String(Files.readAllBytes(Paths.get(fname)));} catch (Exception e) {fail(e);return null;}}
	public static void writeFile(String fname, Object o) { try {Files.write(Paths.get(fname),(""+o).getBytes());} catch (Exception e) {fail(e);}}
	public static boolean eq(Object o1, Object o2){	if (o1 == null && o2 == null) return true; if (o1 == null || o2 == null) return false; return o1.equals(o2);}
	public static boolean empty(String o){ return trim(o).length()==0;}
	public static void print(Object o) {System.out.println(""+o);}
	public static void fail(Object o) {throw new RuntimeException(""+o);}
	public static void warn(Object o) {print(o);}
	public static String nn(Object o) {return o==null?"":""+o;}
	public static String trim(Object o) {return nn(o).trim();}
	public static String[] lines(String str) { return str.split("\r\n|\r|\n");}
}
