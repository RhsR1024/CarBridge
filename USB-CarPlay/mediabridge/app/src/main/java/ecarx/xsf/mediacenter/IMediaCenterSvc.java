package ecarx.xsf.mediacenter;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.Parcelable;
import java.util.List;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public interface IMediaCenterSvc extends IInterface {
    static boolean transact(IBinder remote, int code, Parcel data, Parcel reply, int flags) {
        try {
            if (!remote.transact(code, data, reply, flags)) throw new UnsupportedOperationException("MediaCenter transaction " + code);
            return true;
        } catch (RemoteException error) {
            throw com.geely.auto.music.BridgeFailure.from("MediaCenter transaction " + code, error);
        }
    }
    public static final String DESCRIPTOR = "ecarx.xsf.mediacenter.IMediaCenterSvc";

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static class Default implements IMediaCenterSvc {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean asyncSendVrChannelResult(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder, String str) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean cancelMusicCtrlCapabilityDeclaration(IBinder iBinder) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean cancelNewsCtrlCapabilityDeclaration(IBinder iBinder) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean cancelRadioCtrlCapabilityDeclaration(IBinder iBinder) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean cancelSupportCollectTypes(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean cancelSupportDownloadTypes(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean cancelVrChannelCapability(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public void declareMediaCenterCapability(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr) {
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean declareMusicCtrlCapability(int[] iArr, IBinder iBinder) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean declareNewsCtrlCapability(IBinder iBinder) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean declareRadioCtrlCapability(int[] iArr, IBinder iBinder) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean declareSupportCollectTypes(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean declareSupportDownloadTypes(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean declareVrChannelCapability(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder, IBinder iBinder2) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public void declareVrCtrlPriority(String str, int i, IBinder iBinder, IBinder iBinder2, IBinder iBinder3) {
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public IBinder getMediaControlClientApi() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public IBinder getMediaControllerApi() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public IBinder getStateBinder() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public String queryCurrentFocusClient(IMediaCenterClientToken iMediaCenterClientToken) {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public IMediaCenterClientToken registerInMusic(String str, IMusicClient iMusicClient) {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public IMediaCenterClientToken registerInNews(String str, IBinder iBinder) {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public IMediaCenterClientToken registerInVideo(String str, IBinder iBinder) {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public IMediaCenterClientToken registerMusic(IMusicClient iMusicClient) {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public IMediaCenterClientToken registerNews(IBinder iBinder) {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public IMediaCenterClientToken registerVideo(IBinder iBinder) {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean requestPlay(IMediaCenterClientToken iMediaCenterClientToken) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean unregister(IMediaCenterClientToken iMediaCenterClientToken) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public void updateCollectMsg(IMediaCenterClientToken iMediaCenterClientToken, int i, String str) {
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public void updateCurrentLyric(IMediaCenterClientToken iMediaCenterClientToken, String str) {
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public void updateCurrentProgress(IMediaCenterClientToken iMediaCenterClientToken, long j) {
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean updateCurrentRecommendInfo(IMediaCenterClientToken iMediaCenterClientToken, IRecommend iRecommend) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public void updateCurrentSourceType(IMediaCenterClientToken iMediaCenterClientToken, int i) {
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public void updateErrorMsg(IMediaCenterClientToken iMediaCenterClientToken, int i, String str) {
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean updateMediaContent(IMediaCenterClientToken iMediaCenterClientToken, List list) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public void updateMediaList(IMediaCenterClientToken iMediaCenterClientToken, int i, int i2, List list) {
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public void updateMediaPlayList(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder) {
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public void updateMediaSourceTypeList(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr) {
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean updateMultiMediaList(IMediaCenterClientToken iMediaCenterClientToken, IMediaLists iMediaLists) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean updateMusicPlaybackState(IMediaCenterClientToken iMediaCenterClientToken, IMusicPlaybackInfo iMusicPlaybackInfo) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean updateNewsPlaybackState(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public void updatePlaylist(IMediaCenterClientToken iMediaCenterClientToken, int i, List list) {
        }

        @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
        public boolean updateVideoPlaybackState(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder) {
            return false;
        }
    }

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static abstract class Stub extends Binder implements IMediaCenterSvc {
        static final int TRANSACTION_asyncSendVrChannelResult = 36;
        static final int TRANSACTION_cancelMusicCtrlCapabilityDeclaration = 24;
        static final int TRANSACTION_cancelNewsCtrlCapabilityDeclaration = 28;
        static final int TRANSACTION_cancelRadioCtrlCapabilityDeclaration = 26;
        static final int TRANSACTION_cancelSupportCollectTypes = 16;
        static final int TRANSACTION_cancelSupportDownloadTypes = 18;
        static final int TRANSACTION_cancelVrChannelCapability = 35;
        static final int TRANSACTION_declareMediaCenterCapability = 30;
        static final int TRANSACTION_declareMusicCtrlCapability = 23;
        static final int TRANSACTION_declareNewsCtrlCapability = 27;
        static final int TRANSACTION_declareRadioCtrlCapability = 25;
        static final int TRANSACTION_declareSupportCollectTypes = 15;
        static final int TRANSACTION_declareSupportDownloadTypes = 17;
        static final int TRANSACTION_declareVrChannelCapability = 34;
        static final int TRANSACTION_declareVrCtrlPriority = 22;
        static final int TRANSACTION_getMediaControlClientApi = 32;
        static final int TRANSACTION_getMediaControllerApi = 33;
        static final int TRANSACTION_getStateBinder = 31;
        static final int TRANSACTION_queryCurrentFocusClient = 42;
        static final int TRANSACTION_registerInMusic = 19;
        static final int TRANSACTION_registerInNews = 20;
        static final int TRANSACTION_registerInVideo = 21;
        static final int TRANSACTION_registerMusic = 1;
        static final int TRANSACTION_registerNews = 2;
        static final int TRANSACTION_registerVideo = 3;
        static final int TRANSACTION_requestPlay = 5;
        static final int TRANSACTION_unregister = 4;
        static final int TRANSACTION_updateCollectMsg = 41;
        static final int TRANSACTION_updateCurrentLyric = 14;
        static final int TRANSACTION_updateCurrentProgress = 10;
        static final int TRANSACTION_updateCurrentRecommendInfo = 13;
        static final int TRANSACTION_updateCurrentSourceType = 8;
        static final int TRANSACTION_updateErrorMsg = 37;
        static final int TRANSACTION_updateMediaContent = 38;
        static final int TRANSACTION_updateMediaList = 29;
        static final int TRANSACTION_updateMediaPlayList = 40;
        static final int TRANSACTION_updateMediaSourceTypeList = 7;
        static final int TRANSACTION_updateMultiMediaList = 39;
        static final int TRANSACTION_updateMusicPlaybackState = 6;
        static final int TRANSACTION_updateNewsPlaybackState = 12;
        static final int TRANSACTION_updatePlaylist = 9;
        static final int TRANSACTION_updateVideoPlaybackState = 11;

        /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
        public static class Proxy implements IMediaCenterSvc {
            private IBinder mRemote;

            public Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean asyncSendVrChannelResult(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder, String str) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_asyncSendVrChannelResult, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean cancelMusicCtrlCapabilityDeclaration(IBinder iBinder) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_cancelMusicCtrlCapabilityDeclaration, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean cancelNewsCtrlCapabilityDeclaration(IBinder iBinder) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_cancelNewsCtrlCapabilityDeclaration, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean cancelRadioCtrlCapabilityDeclaration(IBinder iBinder) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_cancelRadioCtrlCapabilityDeclaration, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean cancelSupportCollectTypes(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeIntArray(iArr);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_cancelSupportCollectTypes, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean cancelSupportDownloadTypes(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeIntArray(iArr);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_cancelSupportDownloadTypes, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean cancelVrChannelCapability(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeStrongBinder(iBinder);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_cancelVrChannelCapability, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public void declareMediaCenterCapability(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeIntArray(iArr);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_declareMediaCenterCapability, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean declareMusicCtrlCapability(int[] iArr, IBinder iBinder) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeIntArray(iArr);
                    parcelObtain.writeStrongBinder(iBinder);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_declareMusicCtrlCapability, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean declareNewsCtrlCapability(IBinder iBinder) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_declareNewsCtrlCapability, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean declareRadioCtrlCapability(int[] iArr, IBinder iBinder) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeIntArray(iArr);
                    parcelObtain.writeStrongBinder(iBinder);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_declareRadioCtrlCapability, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean declareSupportCollectTypes(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeIntArray(iArr);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_declareSupportCollectTypes, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean declareSupportDownloadTypes(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeIntArray(iArr);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_declareSupportDownloadTypes, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean declareVrChannelCapability(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder, IBinder iBinder2) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeStrongBinder(iBinder2);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_declareVrChannelCapability, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public void declareVrCtrlPriority(String str, int i, IBinder iBinder, IBinder iBinder2, IBinder iBinder3) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeStrongBinder(iBinder2);
                    parcelObtain.writeStrongBinder(iBinder3);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_declareVrCtrlPriority, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            public String getInterfaceDescriptor() {
                return IMediaCenterSvc.DESCRIPTOR;
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public IBinder getMediaControlClientApi() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_getMediaControlClientApi, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readStrongBinder();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public IBinder getMediaControllerApi() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_getMediaControllerApi, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readStrongBinder();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public IBinder getStateBinder() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_getStateBinder, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readStrongBinder();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public String queryCurrentFocusClient(IMediaCenterClientToken iMediaCenterClientToken) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_queryCurrentFocusClient, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public IMediaCenterClientToken registerInMusic(String str, IMusicClient iMusicClient) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongInterface(iMusicClient);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_registerInMusic, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IMediaCenterClientToken.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public IMediaCenterClientToken registerInNews(String str, IBinder iBinder) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iBinder);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_registerInNews, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IMediaCenterClientToken.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public IMediaCenterClientToken registerInVideo(String str, IBinder iBinder) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iBinder);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_registerInVideo, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IMediaCenterClientToken.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public IMediaCenterClientToken registerMusic(IMusicClient iMusicClient) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMusicClient);
                    IMediaCenterSvc.transact(this.mRemote, 1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IMediaCenterClientToken.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public IMediaCenterClientToken registerNews(IBinder iBinder) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    IMediaCenterSvc.transact(this.mRemote, 2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IMediaCenterClientToken.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public IMediaCenterClientToken registerVideo(IBinder iBinder) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    IMediaCenterSvc.transact(this.mRemote, 3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IMediaCenterClientToken.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean requestPlay(IMediaCenterClientToken iMediaCenterClientToken) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    IMediaCenterSvc.transact(this.mRemote, 5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean unregister(IMediaCenterClientToken iMediaCenterClientToken) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    IMediaCenterSvc.transact(this.mRemote, 4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public void updateCollectMsg(IMediaCenterClientToken iMediaCenterClientToken, int i, String str) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_updateCollectMsg, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public void updateCurrentLyric(IMediaCenterClientToken iMediaCenterClientToken, String str) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeString(str);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_updateCurrentLyric, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public void updateCurrentProgress(IMediaCenterClientToken iMediaCenterClientToken, long j) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeLong(j);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_updateCurrentProgress, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean updateCurrentRecommendInfo(IMediaCenterClientToken iMediaCenterClientToken, IRecommend iRecommend) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeStrongInterface(iRecommend);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_updateCurrentRecommendInfo, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public void updateCurrentSourceType(IMediaCenterClientToken iMediaCenterClientToken, int i) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeInt(i);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_updateCurrentSourceType, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public void updateErrorMsg(IMediaCenterClientToken iMediaCenterClientToken, int i, String str) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_updateErrorMsg, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean updateMediaContent(IMediaCenterClientToken iMediaCenterClientToken, List list) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeList(list);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_updateMediaContent, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public void updateMediaList(IMediaCenterClientToken iMediaCenterClientToken, int i, int i2, List list) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeList(list);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_updateMediaList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public void updateMediaPlayList(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeStrongBinder(iBinder);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_updateMediaPlayList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public void updateMediaSourceTypeList(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeIntArray(iArr);
                    IMediaCenterSvc.transact(this.mRemote, 7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean updateMultiMediaList(IMediaCenterClientToken iMediaCenterClientToken, IMediaLists iMediaLists) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    _Parcel.writeTypedObject(parcelObtain, iMediaLists, 0);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_updateMultiMediaList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean updateMusicPlaybackState(IMediaCenterClientToken iMediaCenterClientToken, IMusicPlaybackInfo iMusicPlaybackInfo) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeStrongInterface(iMusicPlaybackInfo);
                    IMediaCenterSvc.transact(this.mRemote, 6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean updateNewsPlaybackState(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeStrongBinder(iBinder);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_updateNewsPlaybackState, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public void updatePlaylist(IMediaCenterClientToken iMediaCenterClientToken, int i, List list) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeList(list);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_updatePlaylist, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMediaCenterSvc
            public boolean updateVideoPlaybackState(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMediaCenterSvc.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iMediaCenterClientToken);
                    parcelObtain.writeStrongBinder(iBinder);
                    IMediaCenterSvc.transact(this.mRemote, Stub.TRANSACTION_updateVideoPlaybackState, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, IMediaCenterSvc.DESCRIPTOR);
        }

        public static IMediaCenterSvc asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(IMediaCenterSvc.DESCRIPTOR);
            return (iInterfaceQueryLocalInterface == null || !(iInterfaceQueryLocalInterface instanceof IMediaCenterSvc)) ? new Proxy(iBinder) : (IMediaCenterSvc) iInterfaceQueryLocalInterface;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(IMediaCenterSvc.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(IMediaCenterSvc.DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    IMediaCenterClientToken iMediaCenterClientTokenRegisterMusic = registerMusic(IMusicClient.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeStrongInterface(iMediaCenterClientTokenRegisterMusic);
                    return true;
                case 2:
                    IMediaCenterClientToken iMediaCenterClientTokenRegisterNews = registerNews(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeStrongInterface(iMediaCenterClientTokenRegisterNews);
                    return true;
                case 3:
                    IMediaCenterClientToken iMediaCenterClientTokenRegisterVideo = registerVideo(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeStrongInterface(iMediaCenterClientTokenRegisterVideo);
                    return true;
                case 4:
                    boolean zUnregister = unregister(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zUnregister ? 1 : 0);
                    return true;
                case 5:
                    boolean zRequestPlay = requestPlay(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zRequestPlay ? 1 : 0);
                    return true;
                case 6:
                    boolean zUpdateMusicPlaybackState = updateMusicPlaybackState(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), IMusicPlaybackInfo.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zUpdateMusicPlaybackState ? 1 : 0);
                    return true;
                case 7:
                    updateMediaSourceTypeList(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.createIntArray());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_updateCurrentSourceType /* 8 */:
                    updateCurrentSourceType(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_updatePlaylist /* 9 */:
                    updatePlaylist(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readArrayList(getClass().getClassLoader()));
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_updateCurrentProgress /* 10 */:
                    updateCurrentProgress(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.readLong());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_updateVideoPlaybackState /* 11 */:
                    boolean zUpdateVideoPlaybackState = updateVideoPlaybackState(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zUpdateVideoPlaybackState ? 1 : 0);
                    return true;
                case TRANSACTION_updateNewsPlaybackState /* 12 */:
                    boolean zUpdateNewsPlaybackState = updateNewsPlaybackState(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zUpdateNewsPlaybackState ? 1 : 0);
                    return true;
                case TRANSACTION_updateCurrentRecommendInfo /* 13 */:
                    boolean zUpdateCurrentRecommendInfo = updateCurrentRecommendInfo(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), IRecommend.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zUpdateCurrentRecommendInfo ? 1 : 0);
                    return true;
                case TRANSACTION_updateCurrentLyric /* 14 */:
                    updateCurrentLyric(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_declareSupportCollectTypes /* 15 */:
                    boolean zDeclareSupportCollectTypes = declareSupportCollectTypes(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.createIntArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(zDeclareSupportCollectTypes ? 1 : 0);
                    return true;
                case TRANSACTION_cancelSupportCollectTypes /* 16 */:
                    boolean zCancelSupportCollectTypes = cancelSupportCollectTypes(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.createIntArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCancelSupportCollectTypes ? 1 : 0);
                    return true;
                case TRANSACTION_declareSupportDownloadTypes /* 17 */:
                    boolean zDeclareSupportDownloadTypes = declareSupportDownloadTypes(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.createIntArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(zDeclareSupportDownloadTypes ? 1 : 0);
                    return true;
                case TRANSACTION_cancelSupportDownloadTypes /* 18 */:
                    boolean zCancelSupportDownloadTypes = cancelSupportDownloadTypes(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.createIntArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCancelSupportDownloadTypes ? 1 : 0);
                    return true;
                case TRANSACTION_registerInMusic /* 19 */:
                    IMediaCenterClientToken iMediaCenterClientTokenRegisterInMusic = registerInMusic(parcel.readString(), IMusicClient.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeStrongInterface(iMediaCenterClientTokenRegisterInMusic);
                    return true;
                case TRANSACTION_registerInNews /* 20 */:
                    IMediaCenterClientToken iMediaCenterClientTokenRegisterInNews = registerInNews(parcel.readString(), parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeStrongInterface(iMediaCenterClientTokenRegisterInNews);
                    return true;
                case TRANSACTION_registerInVideo /* 21 */:
                    IMediaCenterClientToken iMediaCenterClientTokenRegisterInVideo = registerInVideo(parcel.readString(), parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeStrongInterface(iMediaCenterClientTokenRegisterInVideo);
                    return true;
                case TRANSACTION_declareVrCtrlPriority /* 22 */:
                    declareVrCtrlPriority(parcel.readString(), parcel.readInt(), parcel.readStrongBinder(), parcel.readStrongBinder(), parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_declareMusicCtrlCapability /* 23 */:
                    boolean zDeclareMusicCtrlCapability = declareMusicCtrlCapability(parcel.createIntArray(), parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zDeclareMusicCtrlCapability ? 1 : 0);
                    return true;
                case TRANSACTION_cancelMusicCtrlCapabilityDeclaration /* 24 */:
                    boolean zCancelMusicCtrlCapabilityDeclaration = cancelMusicCtrlCapabilityDeclaration(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCancelMusicCtrlCapabilityDeclaration ? 1 : 0);
                    return true;
                case TRANSACTION_declareRadioCtrlCapability /* 25 */:
                    boolean zDeclareRadioCtrlCapability = declareRadioCtrlCapability(parcel.createIntArray(), parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zDeclareRadioCtrlCapability ? 1 : 0);
                    return true;
                case TRANSACTION_cancelRadioCtrlCapabilityDeclaration /* 26 */:
                    boolean zCancelRadioCtrlCapabilityDeclaration = cancelRadioCtrlCapabilityDeclaration(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCancelRadioCtrlCapabilityDeclaration ? 1 : 0);
                    return true;
                case TRANSACTION_declareNewsCtrlCapability /* 27 */:
                    boolean zDeclareNewsCtrlCapability = declareNewsCtrlCapability(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zDeclareNewsCtrlCapability ? 1 : 0);
                    return true;
                case TRANSACTION_cancelNewsCtrlCapabilityDeclaration /* 28 */:
                    boolean zCancelNewsCtrlCapabilityDeclaration = cancelNewsCtrlCapabilityDeclaration(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCancelNewsCtrlCapabilityDeclaration ? 1 : 0);
                    return true;
                case TRANSACTION_updateMediaList /* 29 */:
                    updateMediaList(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readInt(), parcel.readArrayList(getClass().getClassLoader()));
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_declareMediaCenterCapability /* 30 */:
                    declareMediaCenterCapability(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.createIntArray());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_getStateBinder /* 31 */:
                    IBinder stateBinder = getStateBinder();
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(stateBinder);
                    return true;
                case TRANSACTION_getMediaControlClientApi /* 32 */:
                    IBinder mediaControlClientApi = getMediaControlClientApi();
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(mediaControlClientApi);
                    return true;
                case TRANSACTION_getMediaControllerApi /* 33 */:
                    IBinder mediaControllerApi = getMediaControllerApi();
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(mediaControllerApi);
                    return true;
                case TRANSACTION_declareVrChannelCapability /* 34 */:
                    boolean zDeclareVrChannelCapability = declareVrChannelCapability(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.readStrongBinder(), parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zDeclareVrChannelCapability ? 1 : 0);
                    return true;
                case TRANSACTION_cancelVrChannelCapability /* 35 */:
                    boolean zCancelVrChannelCapability = cancelVrChannelCapability(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCancelVrChannelCapability ? 1 : 0);
                    return true;
                case TRANSACTION_asyncSendVrChannelResult /* 36 */:
                    boolean zAsyncSendVrChannelResult = asyncSendVrChannelResult(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.readStrongBinder(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zAsyncSendVrChannelResult ? 1 : 0);
                    return true;
                case TRANSACTION_updateErrorMsg /* 37 */:
                    updateErrorMsg(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_updateMediaContent /* 38 */:
                    boolean zUpdateMediaContent = updateMediaContent(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.readArrayList(getClass().getClassLoader()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zUpdateMediaContent ? 1 : 0);
                    return true;
                case TRANSACTION_updateMultiMediaList /* 39 */:
                    boolean zUpdateMultiMediaList = updateMultiMediaList(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), (IMediaLists) _Parcel.readTypedObject(parcel, IMediaLists.CREATOR));
                    parcel2.writeNoException();
                    parcel2.writeInt(zUpdateMultiMediaList ? 1 : 0);
                    return true;
                case TRANSACTION_updateMediaPlayList /* 40 */:
                    updateMediaPlayList(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_updateCollectMsg /* 41 */:
                    updateCollectMsg(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_queryCurrentFocusClient /* 42 */:
                    String strQueryCurrentFocusClient = queryCurrentFocusClient(IMediaCenterClientToken.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeString(strQueryCurrentFocusClient);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }
    }

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static class _Parcel {
        /* JADX INFO: Access modifiers changed from: private */
        public static <T> T readTypedObject(Parcel parcel, Parcelable.Creator<T> creator) {
            if (parcel.readInt() != 0) {
                return creator.createFromParcel(parcel);
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static <T extends Parcelable> void writeTypedObject(Parcel parcel, T t, int i) {
            if (t == null) {
                parcel.writeInt(0);
            } else {
                parcel.writeInt(1);
                t.writeToParcel(parcel, i);
            }
        }
    }

    boolean asyncSendVrChannelResult(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder, String str);

    boolean cancelMusicCtrlCapabilityDeclaration(IBinder iBinder);

    boolean cancelNewsCtrlCapabilityDeclaration(IBinder iBinder);

    boolean cancelRadioCtrlCapabilityDeclaration(IBinder iBinder);

    boolean cancelSupportCollectTypes(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr);

    boolean cancelSupportDownloadTypes(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr);

    boolean cancelVrChannelCapability(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder);

    void declareMediaCenterCapability(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr);

    boolean declareMusicCtrlCapability(int[] iArr, IBinder iBinder);

    boolean declareNewsCtrlCapability(IBinder iBinder);

    boolean declareRadioCtrlCapability(int[] iArr, IBinder iBinder);

    boolean declareSupportCollectTypes(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr);

    boolean declareSupportDownloadTypes(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr);

    boolean declareVrChannelCapability(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder, IBinder iBinder2);

    void declareVrCtrlPriority(String str, int i, IBinder iBinder, IBinder iBinder2, IBinder iBinder3);

    IBinder getMediaControlClientApi();

    IBinder getMediaControllerApi();

    IBinder getStateBinder();

    String queryCurrentFocusClient(IMediaCenterClientToken iMediaCenterClientToken);

    IMediaCenterClientToken registerInMusic(String str, IMusicClient iMusicClient);

    IMediaCenterClientToken registerInNews(String str, IBinder iBinder);

    IMediaCenterClientToken registerInVideo(String str, IBinder iBinder);

    IMediaCenterClientToken registerMusic(IMusicClient iMusicClient);

    IMediaCenterClientToken registerNews(IBinder iBinder);

    IMediaCenterClientToken registerVideo(IBinder iBinder);

    boolean requestPlay(IMediaCenterClientToken iMediaCenterClientToken);

    boolean unregister(IMediaCenterClientToken iMediaCenterClientToken);

    void updateCollectMsg(IMediaCenterClientToken iMediaCenterClientToken, int i, String str);

    void updateCurrentLyric(IMediaCenterClientToken iMediaCenterClientToken, String str);

    void updateCurrentProgress(IMediaCenterClientToken iMediaCenterClientToken, long j);

    boolean updateCurrentRecommendInfo(IMediaCenterClientToken iMediaCenterClientToken, IRecommend iRecommend);

    void updateCurrentSourceType(IMediaCenterClientToken iMediaCenterClientToken, int i);

    void updateErrorMsg(IMediaCenterClientToken iMediaCenterClientToken, int i, String str);

    boolean updateMediaContent(IMediaCenterClientToken iMediaCenterClientToken, List list);

    void updateMediaList(IMediaCenterClientToken iMediaCenterClientToken, int i, int i2, List list);

    void updateMediaPlayList(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder);

    void updateMediaSourceTypeList(IMediaCenterClientToken iMediaCenterClientToken, int[] iArr);

    boolean updateMultiMediaList(IMediaCenterClientToken iMediaCenterClientToken, IMediaLists iMediaLists);

    boolean updateMusicPlaybackState(IMediaCenterClientToken iMediaCenterClientToken, IMusicPlaybackInfo iMusicPlaybackInfo);

    boolean updateNewsPlaybackState(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder);

    void updatePlaylist(IMediaCenterClientToken iMediaCenterClientToken, int i, List list);

    boolean updateVideoPlaybackState(IMediaCenterClientToken iMediaCenterClientToken, IBinder iBinder);
}



