import java.io.File;
import java.util.*;
import com.android.tools.smali.baksmali.*;
import com.android.tools.smali.smali.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;

public class BridgeDexTool {
    public static void main(String[] args) throws Exception {
        DexFile base = DexFileFactory.loadDexFile(args[1], null);
        if (args[0].equals("dis")) {
            BaksmaliOptions opts = new BaksmaliOptions(); opts.debugInfo = false;
            List<String> selected = Arrays.asList("Lcom/zqsdk/OooOo00;",
                "Lcn/manstep/phonemirrorBox/v0/d;", "Lcn/manstep/phonemirrorBox/third/ZqUtil;",
                "Lcn/manstep/phonemirrorBox/y;");
            if (!Baksmali.disassembleDexFile(base,new File(args[2]),1,opts,selected)) throw new IllegalStateException("dis");
        } else if (args[0].equals("merge")) {
            SmaliOptions opts = new SmaliOptions(); opts.apiLevel=28; opts.jobs=1; opts.outputDexFile=args[4]+".patch.dex";
            if (!Smali.assemble(opts,args[2])) throw new IllegalStateException("assemble");
            Map<String,ClassDef> classes=new TreeMap<>();
            for(ClassDef c:base.getClasses()) classes.put(c.getType(),c);
            for(ClassDef c:DexFileFactory.loadDexFile(opts.outputDexFile,null).getClasses()) {
                if(!classes.containsKey(c.getType())) throw new IllegalStateException("Unexpected patch class "+c.getType());
                classes.put(c.getType(),c);
            }
            for(ClassDef c:DexFileFactory.loadDexFile(args[3],null).getClasses()) {
                if(classes.containsKey(c.getType())) throw new IllegalStateException("Helper overwrites base "+c.getType());
                if(!c.getType().startsWith("Lcn/manstep/phonemirrorBox/bridge/") && !c.getType().equals("Lio/github/rhsr1024/interop/BridgeProtocol;"))
                    throw new IllegalStateException("Unexpected helper/stub "+c.getType());
                classes.put(c.getType(),c);
            }
            DexFileFactory.writeDexFile(args[4],new ImmutableDexFile(base.getOpcodes(),classes.values()));
        } else throw new IllegalArgumentException(args[0]);
    }
}
