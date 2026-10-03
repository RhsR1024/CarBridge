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
public interface IMusicClient extends IInterface {
    static boolean transact(IBinder remote, int code, Parcel data, Parcel reply, int flags) {
        try { return remote.transact(code, data, reply, flags); } catch (RemoteException error) { return false; }
    }
    public static final String DESCRIPTOR = "ecarx.xsf.mediacenter.IMusicClient";

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static class Default implements IMusicClient {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public int ctrlCollect(int i, boolean z) {
            return 0;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public void ctrlCollectByUUID(int i, String str, boolean z) {
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean ctrlPauseMediaList(int i) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean ctrlPlayMediaList(int i) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public List getContentList() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public long getCurrentProgress() {
            return 0L;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public int getCurrentSourceType() {
            return 0;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public int[] getMediaSourceTypeList() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public IMediaLists getMultiMediaList(int[] iArr) {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public IMusicPlaybackInfo getMusicPlaybackInfo() {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public List getPlaylist(int i) {
            return null;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onCancelRecommend(IRecommend iRecommend) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onCollect(int i, boolean z) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onDownload(int i, boolean z) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onExit() {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onForward() {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onLoopModeChange(int i) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public void onMediaCenterFocusChanged(String str) {
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onMediaForward(boolean z) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onMediaQualityChange(int i) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onMediaRewind(boolean z) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onMediaSelected(IMedia iMedia) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onMediaSelectedPlay(int i, String str) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onNext() {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onPause() {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onPlay() {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onPlayMediaList(int i, int i2) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onPlayRecommend(IRecommend iRecommend) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onPrevious() {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onReplay() {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onRewind() {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public void onSearchMusic(String str, String str2, int i, boolean z, boolean z2, ISearchMusicCallback iSearchMusicCallback) {
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onSeek(long j) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onSourceChanged(int i, String str) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean onSourceSelected(int i) {
            return false;
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public void operationType(int i) {
        }

        @Override // ecarx.xsf.mediacenter.IMusicClient
        public boolean selectListMediaPlay(int i, int i2, String str) {
            return false;
        }
    }

    /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
    public static abstract class Stub extends Binder implements IMusicClient {
        static final int TRANSACTION_ctrlCollect = 32;
        static final int TRANSACTION_ctrlCollectByUUID = 34;
        static final int TRANSACTION_ctrlPauseMediaList = 31;
        static final int TRANSACTION_ctrlPlayMediaList = 30;
        static final int TRANSACTION_getContentList = 28;
        static final int TRANSACTION_getCurrentProgress = 13;
        static final int TRANSACTION_getCurrentSourceType = 12;
        static final int TRANSACTION_getMediaSourceTypeList = 11;
        static final int TRANSACTION_getMultiMediaList = 29;
        static final int TRANSACTION_getMusicPlaybackInfo = 10;
        static final int TRANSACTION_getPlaylist = 14;
        static final int TRANSACTION_onCancelRecommend = 20;
        static final int TRANSACTION_onCollect = 15;
        static final int TRANSACTION_onDownload = 16;
        static final int TRANSACTION_onExit = 26;
        static final int TRANSACTION_onForward = 5;
        static final int TRANSACTION_onLoopModeChange = 7;
        static final int TRANSACTION_onMediaCenterFocusChanged = 25;
        static final int TRANSACTION_onMediaForward = 22;
        static final int TRANSACTION_onMediaQualityChange = 24;
        static final int TRANSACTION_onMediaRewind = 23;
        static final int TRANSACTION_onMediaSelected = 9;
        static final int TRANSACTION_onMediaSelectedPlay = 21;
        static final int TRANSACTION_onNext = 3;
        static final int TRANSACTION_onPause = 2;
        static final int TRANSACTION_onPlay = 1;
        static final int TRANSACTION_onPlayMediaList = 36;
        static final int TRANSACTION_onPlayRecommend = 19;
        static final int TRANSACTION_onPrevious = 4;
        static final int TRANSACTION_onReplay = 18;
        static final int TRANSACTION_onRewind = 6;
        static final int TRANSACTION_onSearchMusic = 35;
        static final int TRANSACTION_onSeek = 37;
        static final int TRANSACTION_onSourceChanged = 17;
        static final int TRANSACTION_onSourceSelected = 8;
        static final int TRANSACTION_operationType = 33;
        static final int TRANSACTION_selectListMediaPlay = 27;

        /* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
        public static class Proxy implements IMusicClient {
            private IBinder mRemote;

            public Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public int ctrlCollect(int i, boolean z) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_ctrlCollect, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public void ctrlCollectByUUID(int i, String str, boolean z) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_ctrlCollectByUUID, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean ctrlPauseMediaList(int i) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_ctrlPauseMediaList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean ctrlPlayMediaList(int i) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_ctrlPlayMediaList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public List getContentList() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_getContentList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readArrayList(getClass().getClassLoader());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public long getCurrentProgress() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_getCurrentProgress, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readLong();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public int getCurrentSourceType() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_getCurrentSourceType, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            public String getInterfaceDescriptor() {
                return IMusicClient.DESCRIPTOR;
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public int[] getMediaSourceTypeList() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_getMediaSourceTypeList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createIntArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public IMediaLists getMultiMediaList(int[] iArr) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeIntArray(iArr);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_getMultiMediaList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (IMediaLists) _Parcel.readTypedObject(parcelObtain2, IMediaLists.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public IMusicPlaybackInfo getMusicPlaybackInfo() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_getMusicPlaybackInfo, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return IMusicPlaybackInfo.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public List getPlaylist(int i) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_getPlaylist, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readArrayList(getClass().getClassLoader());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onCancelRecommend(IRecommend iRecommend) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iRecommend);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onCancelRecommend, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onCollect(int i, boolean z) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onCollect, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onDownload(int i, boolean z) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onDownload, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onExit() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onExit, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onForward() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    IMusicClient.transact(this.mRemote, 5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onLoopModeChange(int i) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    IMusicClient.transact(this.mRemote, 7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public void onMediaCenterFocusChanged(String str) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onMediaCenterFocusChanged, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onMediaForward(boolean z) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onMediaForward, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onMediaQualityChange(int i) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onMediaQualityChange, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onMediaRewind(boolean z) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onMediaRewind, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onMediaSelected(IMedia iMedia) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    _Parcel.writeTypedObject(parcelObtain, iMedia, 0);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onMediaSelected, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onMediaSelectedPlay(int i, String str) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onMediaSelectedPlay, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onNext() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    IMusicClient.transact(this.mRemote, 3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onPause() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    IMusicClient.transact(this.mRemote, 2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onPlay() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    IMusicClient.transact(this.mRemote, 1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onPlayMediaList(int i, int i2) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onPlayMediaList, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onPlayRecommend(IRecommend iRecommend) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeStrongInterface(iRecommend);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onPlayRecommend, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onPrevious() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    IMusicClient.transact(this.mRemote, 4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onReplay() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onReplay, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onRewind() {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    IMusicClient.transact(this.mRemote, 6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public void onSearchMusic(String str, String str2, int i, boolean z, boolean z2, ISearchMusicCallback iSearchMusicCallback) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    parcelObtain.writeStrongInterface(iSearchMusicCallback);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onSearchMusic, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onSeek(long j) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onSeek, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onSourceChanged(int i, String str) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onSourceChanged, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean onSourceSelected(int i) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_onSourceSelected, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public void operationType(int i) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_operationType, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override // ecarx.xsf.mediacenter.IMusicClient
            public boolean selectListMediaPlay(int i, int i2, String str) {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(IMusicClient.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str);
                    IMusicClient.transact(this.mRemote, Stub.TRANSACTION_selectListMediaPlay, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, IMusicClient.DESCRIPTOR);
        }

        public static IMusicClient asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(IMusicClient.DESCRIPTOR);
            return (iInterfaceQueryLocalInterface == null || !(iInterfaceQueryLocalInterface instanceof IMusicClient)) ? new Proxy(iBinder) : (IMusicClient) iInterfaceQueryLocalInterface;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(IMusicClient.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(IMusicClient.DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    boolean zOnPlay = onPlay();
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnPlay ? 1 : 0);
                    return true;
                case 2:
                    boolean zOnPause = onPause();
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnPause ? 1 : 0);
                    return true;
                case 3:
                    boolean zOnNext = onNext();
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnNext ? 1 : 0);
                    return true;
                case 4:
                    boolean zOnPrevious = onPrevious();
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnPrevious ? 1 : 0);
                    return true;
                case 5:
                    boolean zOnForward = onForward();
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnForward ? 1 : 0);
                    return true;
                case 6:
                    boolean zOnRewind = onRewind();
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnRewind ? 1 : 0);
                    return true;
                case 7:
                    boolean zOnLoopModeChange = onLoopModeChange(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnLoopModeChange ? 1 : 0);
                    return true;
                case TRANSACTION_onSourceSelected /* 8 */:
                    boolean zOnSourceSelected = onSourceSelected(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnSourceSelected ? 1 : 0);
                    return true;
                case TRANSACTION_onMediaSelected /* 9 */:
                    boolean zOnMediaSelected = onMediaSelected((IMedia) _Parcel.readTypedObject(parcel, IMedia.CREATOR));
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnMediaSelected ? 1 : 0);
                    return true;
                case TRANSACTION_getMusicPlaybackInfo /* 10 */:
                    IMusicPlaybackInfo musicPlaybackInfo = getMusicPlaybackInfo();
                    parcel2.writeNoException();
                    parcel2.writeStrongInterface(musicPlaybackInfo);
                    return true;
                case TRANSACTION_getMediaSourceTypeList /* 11 */:
                    int[] mediaSourceTypeList = getMediaSourceTypeList();
                    parcel2.writeNoException();
                    parcel2.writeIntArray(mediaSourceTypeList);
                    return true;
                case TRANSACTION_getCurrentSourceType /* 12 */:
                    int currentSourceType = getCurrentSourceType();
                    parcel2.writeNoException();
                    parcel2.writeInt(currentSourceType);
                    return true;
                case TRANSACTION_getCurrentProgress /* 13 */:
                    long currentProgress = getCurrentProgress();
                    parcel2.writeNoException();
                    parcel2.writeLong(currentProgress);
                    return true;
                case TRANSACTION_getPlaylist /* 14 */:
                    List playlist = getPlaylist(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeList(playlist);
                    return true;
                case TRANSACTION_onCollect /* 15 */:
                    boolean zOnCollect = onCollect(parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnCollect ? 1 : 0);
                    return true;
                case TRANSACTION_onDownload /* 16 */:
                    boolean zOnDownload = onDownload(parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnDownload ? 1 : 0);
                    return true;
                case TRANSACTION_onSourceChanged /* 17 */:
                    boolean zOnSourceChanged = onSourceChanged(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnSourceChanged ? 1 : 0);
                    return true;
                case TRANSACTION_onReplay /* 18 */:
                    boolean zOnReplay = onReplay();
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnReplay ? 1 : 0);
                    return true;
                case TRANSACTION_onPlayRecommend /* 19 */:
                    boolean zOnPlayRecommend = onPlayRecommend(IRecommend.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnPlayRecommend ? 1 : 0);
                    return true;
                case TRANSACTION_onCancelRecommend /* 20 */:
                    boolean zOnCancelRecommend = onCancelRecommend(IRecommend.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnCancelRecommend ? 1 : 0);
                    return true;
                case TRANSACTION_onMediaSelectedPlay /* 21 */:
                    boolean zOnMediaSelectedPlay = onMediaSelectedPlay(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnMediaSelectedPlay ? 1 : 0);
                    return true;
                case TRANSACTION_onMediaForward /* 22 */:
                    boolean zOnMediaForward = onMediaForward(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnMediaForward ? 1 : 0);
                    return true;
                case TRANSACTION_onMediaRewind /* 23 */:
                    boolean zOnMediaRewind = onMediaRewind(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnMediaRewind ? 1 : 0);
                    return true;
                case TRANSACTION_onMediaQualityChange /* 24 */:
                    boolean zOnMediaQualityChange = onMediaQualityChange(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnMediaQualityChange ? 1 : 0);
                    return true;
                case TRANSACTION_onMediaCenterFocusChanged /* 25 */:
                    onMediaCenterFocusChanged(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_onExit /* 26 */:
                    boolean zOnExit = onExit();
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnExit ? 1 : 0);
                    return true;
                case TRANSACTION_selectListMediaPlay /* 27 */:
                    boolean zSelectListMediaPlay = selectListMediaPlay(parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zSelectListMediaPlay ? 1 : 0);
                    return true;
                case TRANSACTION_getContentList /* 28 */:
                    List contentList = getContentList();
                    parcel2.writeNoException();
                    parcel2.writeList(contentList);
                    return true;
                case TRANSACTION_getMultiMediaList /* 29 */:
                    IMediaLists multiMediaList = getMultiMediaList(parcel.createIntArray());
                    parcel2.writeNoException();
                    _Parcel.writeTypedObject(parcel2, multiMediaList, 1);
                    return true;
                case TRANSACTION_ctrlPlayMediaList /* 30 */:
                    boolean zCtrlPlayMediaList = ctrlPlayMediaList(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCtrlPlayMediaList ? 1 : 0);
                    return true;
                case TRANSACTION_ctrlPauseMediaList /* 31 */:
                    boolean zCtrlPauseMediaList = ctrlPauseMediaList(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zCtrlPauseMediaList ? 1 : 0);
                    return true;
                case TRANSACTION_ctrlCollect /* 32 */:
                    int iCtrlCollect = ctrlCollect(parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(iCtrlCollect);
                    return true;
                case TRANSACTION_operationType /* 33 */:
                    operationType(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_ctrlCollectByUUID /* 34 */:
                    ctrlCollectByUUID(parcel.readInt(), parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_onSearchMusic /* 35 */:
                    onSearchMusic(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt() != 0, parcel.readInt() != 0, ISearchMusicCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_onPlayMediaList /* 36 */:
                    boolean zOnPlayMediaList = onPlayMediaList(parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnPlayMediaList ? 1 : 0);
                    return true;
                case TRANSACTION_onSeek /* 37 */:
                    boolean zOnSeek = onSeek(parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeInt(zOnSeek ? 1 : 0);
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

    int ctrlCollect(int i, boolean z);

    void ctrlCollectByUUID(int i, String str, boolean z);

    boolean ctrlPauseMediaList(int i);

    boolean ctrlPlayMediaList(int i);

    List getContentList();

    long getCurrentProgress();

    int getCurrentSourceType();

    int[] getMediaSourceTypeList();

    IMediaLists getMultiMediaList(int[] iArr);

    IMusicPlaybackInfo getMusicPlaybackInfo();

    List getPlaylist(int i);

    boolean onCancelRecommend(IRecommend iRecommend);

    boolean onCollect(int i, boolean z);

    boolean onDownload(int i, boolean z);

    boolean onExit();

    boolean onForward();

    boolean onLoopModeChange(int i);

    void onMediaCenterFocusChanged(String str);

    boolean onMediaForward(boolean z);

    boolean onMediaQualityChange(int i);

    boolean onMediaRewind(boolean z);

    boolean onMediaSelected(IMedia iMedia);

    boolean onMediaSelectedPlay(int i, String str);

    boolean onNext();

    boolean onPause();

    boolean onPlay();

    boolean onPlayMediaList(int i, int i2);

    boolean onPlayRecommend(IRecommend iRecommend);

    boolean onPrevious();

    boolean onReplay();

    boolean onRewind();

    void onSearchMusic(String str, String str2, int i, boolean z, boolean z2, ISearchMusicCallback iSearchMusicCallback);

    boolean onSeek(long j);

    boolean onSourceChanged(int i, String str);

    boolean onSourceSelected(int i);

    void operationType(int i);

    boolean selectListMediaPlay(int i, int i2, String str);
}



