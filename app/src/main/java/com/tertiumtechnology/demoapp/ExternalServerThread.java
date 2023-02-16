package com.tertiumtechnology.demoapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ExternalServerThread extends Thread {

    public interface DeviceActivityCommand {
        void excecute(DeviceActivity activity, String inputData);
    }

    public class NetworkBleReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, final Intent intent) {
            new Thread(() -> {
                String dataRead = intent.getStringExtra(BleService.INTENT_EXTRA_DATA_VALUE);

                if (!TextUtils.isEmpty(dataRead)) {
                    sendMsg(dataRead, false);
                }
            }).start();
        }

        private void sendMsg(String msg, boolean ln) {
            if (outputWriter != null) {
                if (ln) {
                    outputWriter.println(msg);
                }
                else {
                    outputWriter.print(msg);
                }
                outputWriter.flush();
            }
        }
    }

    public static final int PORT_DEFAULT_VALUE = 1234;

    private static final String TAG = ExternalServerThread.class.getSimpleName();
    private final int SERVER_SOCKET_ACCEPT_TIMEOUT = 1500;
    private final int SOCKET_READ_TIMEOUT = 1000;
    private final NetworkBleReceiver networkBleReceiver;
    private final WeakReference<DeviceActivity> deviceActivityWeakReference;
    private BufferedReader inputReader;
    private PrintWriter outputWriter = null;
    private Socket socket;

    private final String serverName;
    private final InetAddress address;
    private final int port;
    private DeviceActivityCommand command;

    public ExternalServerThread(String serverName, DeviceActivity deviceActivity, InetAddress address,
                                int port) {
        this.serverName = serverName;
        this.address = address;
        this.port = port;

        deviceActivityWeakReference = new WeakReference<>(deviceActivity);
        networkBleReceiver = new NetworkBleReceiver();
    }

    public ExternalServerThread(String serverName, DeviceActivity deviceActivity, InetAddress address, int port,
                                DeviceActivityCommand command) {
        this(serverName, deviceActivity, address, port);
        this.command = command;
    }

    public NetworkBleReceiver getNetworkBleReceiver() {
        return networkBleReceiver;
    }

    @Override
    public void run() {
        if (deviceActivityWeakReference.get() == null) {
            Log.w(TAG, "DeviceActivity not found");
            return;
        }

        if (address == null) {
            adviceOnUi(getResourceMsg("error_no_wifi_adddress", R.string.error_no_wifi_adddress));
            return;
        }

        ServerSocket serverSocket;

        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(address, port));
            serverSocket.setSoTimeout(SERVER_SOCKET_ACCEPT_TIMEOUT);
        } catch (IOException e) {
            adviceOnUi(getResourceMsg("error_unable_to_start_external_server", R.string
                    .error_unable_to_start_external_server, serverName, e.getMessage()));
            return;
        }

        adviceOnUi(getResourceMsg("external_server_started", R.string.external_server_started, serverName, serverSocket
                .getLocalSocketAddress()));

        while (!Thread.currentThread().isInterrupted()) {
            try {
                socket = serverSocket.accept();
                socket.setSoTimeout(SOCKET_READ_TIMEOUT);
            } catch (SocketTimeoutException e) {
                safeClose(socket);
                continue;
            } catch (IOException e) {
                adviceOnUi(getResourceMsg("error_unable_to_connect_client", R.string.error_unable_to_connect_client,
                        serverName, e.getMessage()));
                e.printStackTrace();

                safeClose(socket);
                break;
            }

            adviceOnUi(getResourceMsg("client_connected", R.string.client_connected, serverName));

            try {
                inputReader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                outputWriter = new PrintWriter(socket.getOutputStream(), true);

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        String input;

                        while ((input = inputReader.readLine()) != null) {
                            final String finalInput = input;

                            runOnUiThread(() -> {
                                if (deviceActivityWeakReference.get() != null && command != null) {
                                    command.excecute(deviceActivityWeakReference.get(), finalInput);
                                }
                            });
                        }

                        adviceOnUi(getResourceMsg("client_connection_lost", R.string.client_connection_lost,
                                serverName));
                        break;
                    } catch (SocketTimeoutException e) {

                    }
                }
            } catch (IOException e) {
                adviceOnUi(getResourceMsg("error_i_o_operation", R.string.error_i_o_operation, e.getMessage()));
                e.printStackTrace();
            } finally {
                safeClose(inputReader);
                safeClose(outputWriter);
                safeClose(socket);
            }
        }

        adviceOnUi(getResourceMsg("external_server_stop", R.string.external_server_stop, serverName));

        safeClose(serverSocket);
    }

    private void adviceOnUi(final String advice) {
        Log.i(TAG, "Advice: " + advice);

        runOnUiThread(() -> {
            if (deviceActivityWeakReference.get() != null) {
                Toast.makeText(deviceActivityWeakReference.get().getApplicationContext(), advice, Toast
                        .LENGTH_SHORT).show();
            }
        });
    }

    private String getResourceMsg(String defaultMsg, int resMsgId, Object... args) {
        if (deviceActivityWeakReference.get() != null) {
            return deviceActivityWeakReference.get().getString(resMsgId, args);
        }
        return defaultMsg;
    }

    private void runOnUiThread(Runnable runnable) {
        if (deviceActivityWeakReference.get() != null) {
            deviceActivityWeakReference.get().runOnUiThread(runnable);
        }
    }

    // workaround for Closeable support only starting from KITKAT (19)
    private <T> void safeClose(T closeable) {
        if (closeable != null) {
            try {
                if (closeable instanceof Reader) {
                    ((Reader) closeable).close();
                }
                if (closeable instanceof Writer) {
                    ((Writer) closeable).close();
                }
                if (closeable instanceof Socket) {
                    ((Socket) closeable).close();
                }
                if (closeable instanceof ServerSocket) {
                    ((ServerSocket) closeable).close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
