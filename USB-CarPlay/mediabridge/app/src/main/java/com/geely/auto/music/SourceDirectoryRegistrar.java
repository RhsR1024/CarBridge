package com.geely.auto.music;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.IBinder;
import dalvik.system.DexClassLoader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Adds only our source to the vehicle's existing directory. Runs off the main thread. */
public final class SourceDirectoryRegistrar {
    private SourceDirectoryRegistrar() {}

    enum Status { PRESENT, ADDED, UPDATED, NOT_READY, UNCONFIRMED, UNAVAILABLE, FAILED, CANCELLED, BUSY, TIMEOUT, WAITING }

    static final class Result {
        final Status status;
        final String message, detail;
        final int ownBefore;
        Result(Status status, String message) { this(status, message, -1, ""); }
        Result(Status status, String message, int ownBefore, String detail) {
            this.status = status; this.message = message; this.ownBefore = ownBefore; this.detail = detail;
        }
        boolean confirmed() { return status == Status.PRESENT || status == Status.ADDED || status == Status.UPDATED; }
        boolean changed() { return status == Status.ADDED || status == Status.UPDATED; }
    }

    interface Directory {
        List<?> read() throws Exception;
        void write(List<Object> sources) throws Exception;
        Object ownSource() throws Exception;
    }

    static Result register(Context context, IBinder easBinder, BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) return new Result(Status.CANCELLED, "来源目录登记已取消");
        if (easBinder == null) return new Result(Status.NOT_READY, "来源目录待登记：EAS 服务未连接");
        try {
            ApplicationInfo factory = context.getPackageManager().getApplicationInfo("com.flyme.auto.music", 0);
            if ((factory.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
                return new Result(Status.UNAVAILABLE, "来源目录不可用：未找到系统媒体接口");
            ClassLoader loader = new DexClassLoader(factory.sourceDir,
                    context.getCodeCacheDir().getAbsolutePath(), null, context.getClassLoader());
            Class<?> serviceType = loader.loadClass("com.ecarx.eas.framework.sdk.common.internal.IEASFrameworkService");
            Object service = loader.loadClass("com.ecarx.eas.framework.sdk.common.internal.EASFrameworkServiceImpl")
                    .getMethod("asInterface", IBinder.class).invoke(null, easBinder);
            Class<?> controllerType = loader.loadClass("ecarx.xsf.mediacenter.session.EasMediaControllerImp");
            Object controller = controllerType.getMethod("get").invoke(null);
            controllerType.getMethod("init", serviceType).invoke(controller, service);
            Method read = controllerType.getMethod("getAppSourceConfig");
            Method write = controllerType.getMethod("setAppSourceConfig", List.class);
            Class<?> builderType = loader.loadClass("ecarx.xsf.mediacenter.session.AppSourceInfo$Builder");
            String pkg = context.getPackageName();
            String name = "媒体桥接";
            String icon = "android.resource://" + pkg + "/" + context.getApplicationInfo().icon;
            return mergeAndVerify(new Directory() {
                public List<?> read() throws Exception { return (List<?>) read.invoke(controller); }
                public void write(List<Object> sources) throws Exception { write.invoke(controller, sources); }
                public Object ownSource() throws Exception {
                    Object builder = builderType.getConstructor().newInstance();
                    builderType.getMethod("packageName", String.class).invoke(builder, pkg);
                    builderType.getMethod("appName", String.class).invoke(builder, name);
                    builderType.getMethod("iconPath", String.class).invoke(builder, icon);
                    builderType.getMethod("sourceTypeList", int[].class).invoke(builder, new int[]{6});
                    builderType.getMethod("priorityLevel", int.class).invoke(builder, 1);
                    return builderType.getMethod("build").invoke(builder);
                }
            }, pkg, cancelled);
        } catch (Exception | LinkageError error) {
            DiagnosticsLog.e("SOURCE_DIRECTORY registration unavailable", error);
            return new Result(Status.FAILED, "来源目录登记失败：" + error.getClass().getSimpleName());
        }
    }

    static Result mergeAndVerify(Directory directory, String ownPackage, BooleanSupplier cancelled) throws Exception {
        if (cancelled.getAsBoolean()) return new Result(Status.CANCELLED, "来源目录登记已取消");
        List<?> original = directory.read();
        if (cancelled.getAsBoolean()) return new Result(Status.CANCELLED, "来源目录登记已取消");
        // An empty read can mean an unready service. Never replace the factory directory with a singleton.
        if (original == null || original.isEmpty())
            return new Result(Status.NOT_READY, "来源目录待登记：车机目录尚未就绪");
        Object own = directory.ownSource();
        if (!ownPackage.equals(packageOf(own))) throw new IllegalArgumentException("Source identity mismatch");
        List<Object> merged = new ArrayList<>();
        Set<String> preserved = new HashSet<>();
        int matching = 0;
        boolean alreadyCorrect = false;
        for (Object source : original) {
            String pkg = packageOf(source);
            if (ownPackage.equals(pkg)) {
                matching++;
                alreadyCorrect = sameSource(source, own);
                if (matching == 1) merged.add(own);
            } else {
                merged.add(source);
                preserved.add(pkg);
            }
        }
        String before = "beforeCount=" + original.size() + " ownBefore=" + matching;
        if (matching == 1 && alreadyCorrect)
            return new Result(Status.PRESENT, "来源目录已登记：原条目存在，复核正常", matching, before + " wrote=false");
        if (matching == 0) merged.add(own);
        if (cancelled.getAsBoolean()) return new Result(Status.CANCELLED, "来源目录登记已取消");
        directory.write(merged);
        if (cancelled.getAsBoolean()) return new Result(Status.CANCELLED, "来源目录登记已取消");
        List<?> readback = directory.read();
        boolean found = false;
        int ownAfter = 0;
        if (readback != null) for (Object source : readback) {
            String pkg = packageOf(source);
            preserved.remove(pkg);
            if (ownPackage.equals(pkg)) {
                ownAfter++;
                if (sameSource(source, own)) found = true;
            }
        }
        String detail = before + " wrote=true afterCount=" + (readback == null ? -1 : readback.size())
                + " ownAfter=" + ownAfter + " missingOtherCount=" + preserved.size();
        if (!found || ownAfter != 1 || !preserved.isEmpty())
            return new Result(Status.UNCONFIRMED, "来源目录登记未确认：车机读回不一致", matching, detail);
        return new Result(matching == 0 ? Status.ADDED : Status.UPDATED,
                matching == 0 ? "来源目录已登记：本次补入并读回确认" : "来源目录已登记：本次修正并读回确认", matching, detail);
    }

    private static Object get(Object source, String method) throws Exception {
        if (source == null) throw new IllegalArgumentException("Null source in directory");
        return source.getClass().getMethod(method).invoke(source);
    }
    private static String packageOf(Object source) throws Exception {
        Object value = get(source, "getPackageName");
        if (!(value instanceof String) || ((String) value).isEmpty()) throw new IllegalArgumentException("Invalid source package");
        return (String) value;
    }
    private static boolean sameSource(Object left, Object right) throws Exception {
        return java.util.Objects.equals(get(left, "getAppName"), get(right, "getAppName"))
                && java.util.Objects.equals(get(left, "getIconPath"), get(right, "getIconPath"))
                && java.util.Objects.equals(get(left, "getPriorityLevel"), get(right, "getPriorityLevel"))
                && Arrays.equals((int[]) get(left, "getSourceTypeList"), (int[]) get(right, "getSourceTypeList"));
    }
}
