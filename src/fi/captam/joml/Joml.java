package fi.captam.joml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Joml {
	public static Joml parse(String...fnames) {
		Joml j = new Joml();
		for (String fname:fnames) j._parse(fname);
		j._validate();
		return j;
	}
	Map<String,String> inherits = new LinkedHashMap<>();
	Map<String,String> map = new LinkedHashMap<>();
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
				map.put(key,value);
				line="";continue;
			}
			fail("invalid line: "+linenum+" in file:"+fname+" : "+line);
		}
	}
	void _validate() { for (String key:map.keySet()) { String val = get(key);}}
	String env_name = null;
	String env_num = null;
	void env(String name, String num) { env_name = name; env_num = num;}
	public String get(String key) {
		int idx = key.lastIndexOf('.'); if (idx<0) return get("",key);
		return get(key.substring(0,idx+1),key.substring(idx+1));
	}
	public String get(String prefix, String key) {
		if (eq("env-name",key)) return env_name;
		if (eq("env-num",key)) return ""+env_num;
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
		if (val==null) fail("not found: "+fk);
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
	public String evalFile(String fname) {
		StringBuilder out = new StringBuilder();
		for (String line:lines(readFile(fname))) { out.append(evalLine("",line)+"\n"); }
		return out.toString();
	}

	public static void main(String args[]) {run(args);}
	public static Joml run(String...args) {
		String env=null,num=null,dirname=null,dumpfname = null;
		List<String> fnames = new ArrayList<>();
		for (int i=0;i<args.length;i++) {
			if (eq(args[i],"--dir")) { dirname = opt(args,++i); continue;}
			if (eq(args[i],"--env")) { env = opt(args,++i); continue;}
			if (eq(args[i],"--num")) { num = opt(args,++i); continue;}
			if (eq(args[i],"--dump")) { dumpfname = opt(args,++i); continue;}
			if (args[i].matches(".*\\.(joml|yml|jml)$")) {fnames.add(args[i]);continue;}
			usage("invalid arg: "+args[i]);
		}
		if (fnames.size()==0) usage("no joml files given");
		Joml j = Joml.parse(fnames.toArray(new String[fnames.size()]));
		if (dirname!=null) {
			File dir = new File(dirname);
			if (!dir.isDirectory()) usage("invalid directory: "+dirname);
			j.env(env,num);
			j.evalDir(dir);
		}
		return j;
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
	public static void fail(Object o) {print(o);throw new RuntimeException(""+o);}
	public static String nn(Object o) {return o==null?"":""+o;}
	public static String trim(Object o) {return nn(o).trim();}
	public static String[] lines(String str) { return str.split("\r\n|\r|\n");}
}
