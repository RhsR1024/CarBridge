import java.io.File;
import java.util.*;
import com.android.tools.smali.baksmali.*;
import com.android.tools.smali.smali.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;

public class DexTool {
    public static void main(String[] args) throws Exception {
        if (args[0].equals("dis")) {
            DexFile dex = DexFileFactory.loadDexFile(args[1], null);
            BaksmaliOptions opts = new BaksmaliOptions();
            opts.debugInfo = false;
            List<String> selected = Arrays.asList(
                "Lcom/zqsdk/OooOo00;", "Lcom/zqsdk/OooOo0;",
                "Lcom/zqsdk/OooOo00$OooO0O0;",
                "Lcn/manstep/phonemirrorBox/third/ZqUtil$c;",
                "Lcn/manstep/phonemirrorBox/e0/c$a$a;");
            if (!Baksmali.disassembleDexFile(dex, new File(args[2]), 1, opts, selected))
                throw new IllegalStateException("Disassembly failed");
        } else if (args[0].equals("merge")) {
            SmaliOptions opts = new SmaliOptions();
            opts.apiLevel = 28;
            opts.jobs = 1;
            opts.outputDexFile = args[3] + ".patch.dex";
            if (!Smali.assemble(opts, args[2])) throw new IllegalStateException("Assembly failed");
            DexFile base = DexFileFactory.loadDexFile(args[1], null);
            DexFile patch = DexFileFactory.loadDexFile(opts.outputDexFile, null);
            Map<String, ClassDef> classes = new TreeMap<>();
            for (ClassDef c : base.getClasses()) classes.put(c.getType(), c);
            for (ClassDef c : patch.getClasses()) {
                if (!classes.containsKey(c.getType())) throw new IllegalStateException(c.getType());
                classes.put(c.getType(), c);
            }
            DexFileFactory.writeDexFile(args[3], new ImmutableDexFile(base.getOpcodes(), classes.values()));
        } else throw new IllegalArgumentException(args[0]);
    }
}
