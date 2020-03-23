package fi.captam.joml;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;

// simple replacement for junit ... only asserts needed anyway and runner is 3 lines..
public class JomlTest {

	public static void main(String args[]) throws Exception {
		int count = 0;
		for (Method m:JomlTest.class.getDeclaredMethods()) {
			if (m.getName().startsWith("test")) {m.invoke(null); print(m.getName()+":ok");count++;}
		}
		print("all tests ok count: "+count);
	}

	public static void test() throws Exception {
		Joml j = Joml.run("env.joml","secrets.joml");
		assertEquals(j.get("key1.passwd"),"password123");
		assertEquals(j.get("key1.txt1"),"key1 txt1");
		assertEquals(j.get("key1.txt2"),"key1 txt2");
	}

	public static void testDirs() throws Exception {
		Files.deleteIfExists(Paths.get("tmpl/tst.txt"));
		Joml j = Joml.run("env.joml","secrets.joml","--dir","tmpl");
		assertTrue(Files.exists(Paths.get("tmpl/tst.txt")));
		assertFileContains("tmpl/tst.txt","key1.txt1=key1 txt1");
		assertFileContains("tmpl/dir1/foo.txt","map2.host=localhost-foobar\n");
		assertFileContains("tmpl/dir1/dir11/bar.txt","server=77\n");
	}

	public static void testEnv() throws Exception {
		Joml j = Joml.run("env.joml","secrets.joml","--dir","tmpl","--env","test","--num","1");
		assertTrue(Files.exists(Paths.get("tmpl/tst.txt")));
		assertFileContains("tmpl/tst.txt","test=this is env1");
	}

	public static void testDump() throws Exception {
		Joml j = Joml.run("env.joml","secrets.joml","--dump","tmpl/keys.dump");
		assertFileContains("tmpl/keys.dump","map1.simple.foo");
	}

	public static void testIf() throws Exception {
		Joml j = Joml.run("env.joml","--dir","tmpl","--env","test","--num","1");
		assertFileContains("tmpl/tst.txt","hello not prod");
		assertFileContains("tmpl/tst.txt","always here");
		assertFileContains("tmpl/tst.txt","always here 2");
		assertFileContainsNot("tmpl/tst.txt","hello prod");
		j = Joml.run("env.joml","--dir","tmpl","--env","prod","--num","1");
		assertFileContains("tmpl/tst.txt","hello prod");
		assertFileContainsNot("tmpl/tst.txt","hello not prod");
		assertFileContains("tmpl/tst.txt","always here");
		assertFileContains("tmpl/tst.txt","always here 2");
		j = Joml.run("env.joml","--dir","tmpl","--env","prod","--num","1","--node","2");
		assertFileContains("tmpl/tst.txt","node2");
	}

	public static void testEscape() throws Exception {
		Joml j = Joml.run("env.joml","--dir","tmpl","--env","test","--num","1");
		assertFileContains("tmpl/tst.txt","$(notvar");
	}

	public static void testOverride() throws Exception {
		Joml j = Joml.run("env.joml","override.joml");
		assertContains(j.get("map1.hello"),"override");
		assertContains(j.get("map1.key1"),"override");
		assertContains(j.get("map1.hello2"),"again");
	}

	static void assertFileContains(String fname, String str) throws Exception { if (!Joml.readFile(fname).contains(str)) throw new Exception();}
	static void assertFileContainsNot(String fname, String str) throws Exception { if (Joml.readFile(fname).contains(str)) throw new Exception();}
	static void assertEquals(Object o1, Object o2) throws Exception { if (!eq(o1,o2)) throw new Exception();}
	static void assertContains(String o1, String o2) throws Exception { if (!o1.contains(o2)) throw new Exception();}
	static void assertTrue(boolean v) throws Exception {if(!v) throw new Exception();}
	public static boolean eq(Object o1, Object o2){	if (o1 == null && o2 == null) return true; if (o1 == null || o2 == null) return false; return o1.equals(o2);}
	public static void fail(Object o) {print(o);throw new RuntimeException(""+o);}
	public static void print(Object o) {System.out.println(""+o);}

}
