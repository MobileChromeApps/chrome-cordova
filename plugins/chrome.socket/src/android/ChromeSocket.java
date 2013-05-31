// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.google.cordova;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.cordova.CordovaArgs;
import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.api.CordovaPlugin;
import org.apache.cordova.api.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class ChromeSocket extends CordovaPlugin {

    private static final String LOG_TAG = "ChromeSocket";

    private static Map<Integer, SocketData> sockets = new HashMap<Integer, SocketData>();
    private static int nextSocket = 1;

    @Override
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        if ("create".equals(action)) {
            create(args, callbackContext);
            return true;
        } else if ("connect".equals(action)) {
            connect(args, callbackContext);
            return true;
        } else if ("bind".equals(action)) {
            bind(args, callbackContext);
            return true;
        } else if ("write".equals(action)) {
            write(args, callbackContext);
            return true;
        } else if ("read".equals(action)) {
            read(args, callbackContext);
            return true;
        } else if ("sendTo".equals(action)) {
            sendTo(args, callbackContext);
            return true;
        } else if ("recvFrom".equals(action)) {
            recvFrom(args, callbackContext);
            return true;
        } else if ("disconnect".equals(action)) {
            disconnect(args, callbackContext);
            return true;
        } else if ("destroy".equals(action)) {
            destroy(args, callbackContext);
            return true;
        } else if ("listen".equals(action)) {
            listen(args, callbackContext);
            return true;
        } else if ("accept".equals(action)) {
            accept(args, callbackContext);
            return true;
        }
        return false;
    }


    private void create(CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        String socketType = args.getString(0);
        if (socketType.equals("tcp") || socketType.equals("udp")) {
            SocketData sd = new SocketData(socketType.equals("tcp") ? SocketData.Type.TCP : SocketData.Type.UDP);
            int id = addSocket(sd);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, id));
        } else {
            Log.e(LOG_TAG, "Unknown socket type: " + socketType);
            // REVIEW(mmocny): Send plugin error result, probably (same for other cases below, only commenting once
        }
    }

    private static int addSocket(SocketData sd) {
        sockets.put(Integer.valueOf(nextSocket), sd);
        return nextSocket++;
    }

    private void connect(CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        int socketId = args.getInt(0);
        String address = args.getString(1);
        int port = args.getInt(2);

        SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            Log.e(LOG_TAG, "No socket with socketId " + socketId);
            // REVIEW(mmocny): This code block repeats often in following functions, consider writing a helper
            // REVIEW(mmocny): That helper should also accept callbackContext and send an error result
            return;
        }

        boolean success = sd.connect(address, port);
        if (success) {
            callbackContext.success();
        } else {
            callbackContext.error("Failed to connect");
        }
    }

    private void bind(CordovaArgs args, final CallbackContext context) throws JSONException {
        int socketId = args.getInt(0);
        String address = args.getString(1);
        int port = args.getInt(2);

        SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            Log.e(LOG_TAG, "No socket with socketId " + socketId);
            return;
        }

        boolean success = sd.bind(address, port);
        if (success) {
            context.success();
        } else {
            context.error("Failed to bind.");
        }
    }

    private void write(CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        int socketId = args.getInt(0);
        byte[] data = args.getArrayBuffer(1);

        SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            Log.e(LOG_TAG, "No socket with socketId " + socketId);
            return;
        }

        int result = sd.write(data);
        if (result <= 0) { // REVIEW(mmocny): What if bytes written < data length?
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, result));
        } else {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
        }
    }

    private void read(CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        int socketId = args.getInt(0);
        int bufferSize = args.getInt(1);

        SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            Log.e(LOG_TAG, "No socket with socketId " + socketId);
            return;
        }

        // Will call the callback once it has some data.
        // REVIEW(mmocny): Nit: everywhere else SocketData helper is isolated from callbackContext and other cordova plugin internals.
        // REVIEW(mmocny): I see why doing this is easier, but it would be better if you had a generic callback and handled the plugin reply complexities here.
        sd.read(bufferSize, callbackContext);
    }

    private void sendTo(CordovaArgs args, final CallbackContext context) throws JSONException {
        JSONObject opts = args.getJSONObject(0);
        int socketId = opts.getInt("socketId");
        String address = opts.getString("address");
        int port = opts.getInt("port");
        byte[] data = args.getArrayBuffer(1);

        SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            Log.e(LOG_TAG, "No socket with socketId " + socketId);
            return;
        }

        int result = sd.sendTo(data, address, port);
        if (result <= 0) {
            context.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, result));
        } else {
            context.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
        }
    }

    private void recvFrom(CordovaArgs args, final CallbackContext context) throws JSONException {
        int socketId = args.getInt(0);
        int bufferSize = args.getInt(1);

        SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            Log.e(LOG_TAG, "No socket with socketId " + socketId);
            return;
        }

        sd.recvFrom(bufferSize, context);
    }

    private void disconnect(CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        int socketId = args.getInt(0);

        SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            Log.e(LOG_TAG, "No socket with socketId " + socketId);
            return;
        }

        sd.disconnect();
        callbackContext.success();
    }

    private void destroy(CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        int socketId = args.getInt(0);

        SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            Log.e(LOG_TAG, "No socket with socketId " + socketId);
            return;
        }

        sd.destroy();
        sockets.remove(Integer.valueOf(socketId));
        callbackContext.success();
    }


    private void listen(CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        int socketId = args.getInt(0);
        String address = args.getString(1);
        int port = args.getInt(2);
        int backlog = args.getInt(3);

        SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            Log.e(LOG_TAG, "No socket with socketId " + socketId);
            return;
        }

        boolean success = sd.listen(address, port, backlog);
        if (success) {
            callbackContext.success();
        } else {
            callbackContext.error("Failed to listen()");
        }
    }

    private void accept(CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        int socketId = args.getInt(0);

        SocketData sd = sockets.get(Integer.valueOf(socketId));
        if (sd == null) {
            Log.e(LOG_TAG, "No socket with socketId " + socketId);
            return;
        }

        sd.accept(callbackContext);
    }


    private static class SocketData {
        private Socket tcpSocket;
        private DatagramSocket udpSocket;
        private ServerSocket serverSocket;

        public enum Type { TCP, UDP; }
        private Type type;

        // Cached values used by UDP read()/write().
        private InetAddress address;
        private int port;
        private boolean connected = false; // Only applies to UDP, where connect() restricts who the socket will receive from.
        private boolean bound = false;

        private boolean isServer = false;

        private BlockingQueue<ReadData> readQueue;
        private ReadThread readThread;

        private BlockingQueue<AcceptData> acceptQueue;
        private AcceptThread acceptThread;


        public SocketData(Type type) {
            this.type = type;
        }

        public SocketData(Socket incoming) {
            this.type = Type.TCP;
            this.tcpSocket = incoming;
            prepareForRead();
        }

        public boolean connect(String address, int port) {
            // REVIEW(mmocny): There are expected failure codes for this.  One day we should research and update iOS and Android to simulate Desktop at least from the common errors.
            if (isServer) return false;
            try {
                // REVIEW(mmocny): What about moving construction of sockets to the constructor?  Would fail earlier and would prevent the need for split init inside Connect/SendTo
                if (type == Type.TCP) {
                    // REVIEW(mmocny): Is there a reason you check udpSocket for null before assignment below, but you don't for tcp?
                    tcpSocket = new Socket(address, port);
                    // REVIEW(mmocny): are this.connected etc members only ever used for UDP?
                    // REVIEW(mmocny): may still want to set them for both types just for least surprise (may also be useful when implementing getInfo)
                } else {
                    if (udpSocket == null) {
                        boolean success = udpInit();
                        if (!success) return false;
                    }
                    this.address = InetAddress.getByName(address);
                    this.port = port;
                    this.connected = true;
                    udpSocket.connect(this.address, port);
                }
                prepareForRead();
            } catch(UnknownHostException uhe) {
                Log.e(LOG_TAG, "Unknown host exception while connecting socket", uhe);
                return false;
            } catch(IOException ioe) {
                Log.e(LOG_TAG, "IOException while connecting socket", ioe);
                return false;
            }
            return true;
        }

        private void prepareForRead() {
            readQueue = new LinkedBlockingQueue<ReadData>();
            readThread = new ReadThread();
            readThread.start();
        }

        private boolean udpInit() {
            try {
                udpSocket = new DatagramSocket();
            } catch (SocketException se) {
                Log.w(LOG_TAG, "SocketException while trying to create a UDP socket", se);
                return false;
            }
            prepareForRead();
            return true;
        }

        public boolean bind(String address, int port) {
            if (type != Type.UDP) {
                Log.e(LOG_TAG, "bind() cannot be called on TCP sockets.");
                return false;
            }

            if (udpSocket == null) {
                boolean success = udpInit();
                if (!success) return false;
            }

            try {
              udpSocket.bind(new InetSocketAddress(port));
            } catch (SocketException se) {
                Log.e(LOG_TAG, "Failed to bind UDP socket.", se);
                // REVIEW(mmocny): context.error?
                return false;
            }

            this.bound = true;
            return true;
        }

        public int write(byte[] data) throws JSONException {
            if ((tcpSocket == null && udpSocket == null) || isServer) return -1;

            int bytesWritten = 0;
            try {
                if (type == Type.TCP) {
                    tcpSocket.getOutputStream().write(data);
                    bytesWritten = data.length;
                } else {
                    if (!connected) {
                        Log.e(LOG_TAG, "Cannot write() to unconnected UDP socket.");
                        return -1;
                    }

                    DatagramPacket packet = new DatagramPacket(data, data.length, this.address, this.port);
                    udpSocket.send(packet);
                    bytesWritten = data.length;
                }
            } catch(IOException ioe) {
                Log.w(LOG_TAG, "IOException while writing to socket", ioe);
                bytesWritten = -1;
            }

            return bytesWritten;
        }

        public int sendTo(byte[] data, String address, int port) {
            if (type != Type.UDP) {
                Log.w(LOG_TAG, "sendTo() can only be called for UDP sockets.");
                return -1;
            }

            // Create the socket and initialize the reading side, if connect() was never called.
            if (udpSocket == null) {
                boolean success = udpInit();
                if (!success) return -1;
            }

            int bytesWritten = 0;
            try {
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(address), port);
                udpSocket.send(packet);
                bytesWritten = data.length;
            } catch (IOException ioe) {
                Log.w(LOG_TAG, "IOException in sendTo", ioe);
                bytesWritten = -1;
            }

            return bytesWritten;
        }

        public void read(int bufferLength, CallbackContext context) {
            // REVIEW(mmocny): If you remove callbackcontext dependancy, can change this to return bool for the cases you test synchronously
            if (isServer) {
                context.error("read() is not allowed on server sockets");
                return;
            }

            if (type == Type.UDP && !bound) {
                context.error("read() is not allowed on unbound UDP sockets.");
                return;
            }

            synchronized(readQueue) {
                try {
                    // REVIEW(mmocny): what if bufferLength is negative?  I think the read handler expects >= 0
                    readQueue.put(new ReadData(bufferLength, context));
                } catch (InterruptedException e) {
                    // REVIEW(mmocny): context.error?
                    e.printStackTrace();
                }
            }
        }

        public void recvFrom(int bufferSize, CallbackContext context) {
            if (type != Type.UDP) {
                context.error("recvFrom() is not allowed on non-UDP sockets");
                return;
            }

            if (!bound) {
                context.error("Cannot recvFrom() without bind() first.");
                return;
            }

            synchronized(readQueue) {
                try {
                    // Flagged as recvFrom, therefore the two-part callback.
                    // REVIEW(mmocny): again, this is nasty since we are leaking conext details into the helper, if we add multiargument returns to android plugin results, we will need to change a lot of this code instead of just the ChromeSocket entry point.
                    readQueue.put(new ReadData(bufferSize, context, true));
                } catch (InterruptedException e) {
                    // REVIEW(mmocny): context.error?
                    e.printStackTrace();
                }
            }
        }

        public void disconnect() {
            try {
                if (isServer) {
                    // acceptQueue is null when listen has been called but not accept().
                    if (acceptQueue != null) {
                        acceptQueue.put(new AcceptData(true));
                    }
                    serverSocket.close();
                } else if (readQueue != null) {
                    readQueue.put(new ReadData(true));
                    if(type == Type.TCP) {
                        tcpSocket.close(); // REVIEW(mmocny): since you only create these on connect/sendTo, this may be unsafe (you can call disconnect whenever).  I suggest just creating the objects in constructor instead of checking state here.  Also, not sure if you need to check isClosed() to call disconnect twice..
                    } else {
                        udpSocket.close();
                    }
                }
            } catch (IOException ioe) {
            } catch (InterruptedException ie) {
            }
            // REVIEW(mmocny): don't need to reset these here if you make the change to create objects in constructor
            tcpSocket = null;
            udpSocket = null;
            serverSocket = null;
            // REVIEW(mmocny): isServer = false, connected = false, address/port etc
            // REVIEW(mmocny): Also, when do we reset queue's?
        }

        public void destroy() {
            // REVIEW(mmocny): if you make the change to construction time, will need to check here for local connected/isServer state etc
            if (tcpSocket != null || udpSocket != null || serverSocket != null) {
                disconnect();
            }
        }

        public boolean listen(String address, int port, int backlog) {
            if (type != Type.TCP) return false;
            // REVIEW(mmocny): what if you already called connect or listen before?
            isServer = true;

            try {
                serverSocket = new ServerSocket(port, backlog);
            } catch (IOException ioe) {
                Log.e(LOG_TAG, "Error creating server socket", ioe);
                return false;
            }
            return true;
        }

        public void accept(CallbackContext context) {
            if (!isServer) {
                // REVIEW(mmocny): We should share this error text across iOS/Android -- perhaps we need a platform agnostic string assets solution, at least for the stuff that prints to js console, android/iOS logs can be custom.
                context.error("accept() is not supported on client sockets. Call listen() first.");
                return;
            }

            // REVIEW(mmocny): should move this to a helper just like we have for prepareForRead
            if (acceptQueue == null && acceptThread == null) {
                acceptQueue = new LinkedBlockingQueue<AcceptData>();
                acceptThread = new AcceptThread();
                acceptThread.start();
            }

            synchronized(acceptQueue) {
                try {
                    acceptQueue.put(new AcceptData(context));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    // REVIEW(mmocny): context.error?
                }
            }
        }

        private static class ReadData {
            public int size;
            public boolean killThread;
            public boolean recvFrom;
            public CallbackContext context;

            public ReadData(int size, CallbackContext context) {
                this.size = size;
                this.context = context;
                this.killThread = false;
                this.recvFrom = false;
            }

            public ReadData(int size, CallbackContext context, boolean recvFrom) {
                this.size = size;
                this.context = context;
                this.killThread = false;
                this.recvFrom = recvFrom;
            }

            public ReadData(boolean killThread) {
                this.killThread = true;
            }
        }


        private class ReadThread extends Thread {
            public void run() {
                try {
                    while (true) {
                        // Read from the blocking queue
                        ReadData readData = SocketData.this.readQueue.take();
                        if(readData.killThread) return;

                        int toRead = readData.size;
                        byte[] out;
                        int bytesRead;

                        if (type == Type.TCP) {
                            if (toRead > 0) {
                                out = new byte[toRead];
                                bytesRead = SocketData.this.tcpSocket.getInputStream().read(out);
                            } else {
                                int firstByte = SocketData.this.tcpSocket.getInputStream().read();
                                // REVIEW(mmocny): What if 0 available? will read with 0 length work as expected?
                                out = new byte[SocketData.this.tcpSocket.getInputStream().available() + 1];
                                out[0] = (byte) firstByte;
                                bytesRead = 1 + SocketData.this.tcpSocket.getInputStream().read(out, 1, out.length - 1);
                            }

                            // Check for EOF
                            // REVIEW(mmocny): if bytesRead < 0 is already treates as EOF maybe you don't need a special killThread flag, and can just set expected read size to -1?
                            if (bytesRead < 0) {
                                SocketData.this.disconnect();
                                // REVIEW(mmocny): context.error?
                                return;
                            }

                            readData.context.success(out);
                        } else {
                            if (toRead > 0) {
                                out = new byte[toRead];
                            } else {
                                out = new byte[4096]; // Defaults to 4K chunks.
                            }

                            DatagramPacket packet = new DatagramPacket(out, out.length);
                            udpSocket.receive(packet);

                            // Truncate the buffer if the message was shorter than it.
                            if (packet.getLength() != out.length) {
                                byte[] temp = new byte[packet.getLength()];
                                for(int i = 0; i < packet.getLength(); i++) {
                                    temp[i] = out[i];
                                }
                                out = temp;
                            }

                            PluginResult dataResult = new PluginResult(PluginResult.Status.OK, out);
                            // If this was a recvFrom() call, keep the callback for the two-part response.
                            // If this was a read() call, don't keep the callback.
                            dataResult.setKeepCallback(readData.recvFrom);
                            readData.context.sendPluginResult(dataResult);

                            if (readData.recvFrom) {
                                JSONObject obj = new JSONObject();
                                try {
                                    obj.put("address", packet.getAddress().getHostAddress());
                                    obj.put("port", packet.getPort());
                                } catch (JSONException je) {
                                    Log.e(LOG_TAG, "Error constructing JSON object to return from recvFrom()", je);
                                    // REVIEW(mmocny): context.error?
                                    // REVIEW(mmocny): Also, since this can fail, you will need to delay sending the first plugin result until you know this doesn't fail
                                    return;
                                }
                                readData.context.success(obj);
                            }
                        }
                    } // while
                } catch (IOException ioe) {
                    Socket s = SocketData.this.tcpSocket;
                    if (s != null && s.isClosed()) {
                        Log.i(LOG_TAG, "Socket closed.");
                    } else {
                        Log.w(LOG_TAG, "Failed to read from socket.", ioe);
                    }
                    // REVIEW(mmocny): context.error? for all remaining ReadData in queue?
                } catch (InterruptedException ie) {
                    Log.w(LOG_TAG, "Thread interrupted", ie);
                    // REVIEW(mmocny): context.error? for all remaining ReadData in queue?
                }
            } // run()
        } // ReadThread

        private static class AcceptData {
            public boolean killThread;
            public CallbackContext context;

            public AcceptData(boolean killThread) {
                this.killThread = killThread;
            }

            public AcceptData(CallbackContext context) {
                this.context = context;
                this.killThread = false;
            }
        }

        private class AcceptThread extends Thread {
            public void run() {
                try {
                    while(true) {
                        AcceptData acceptData = SocketData.this.acceptQueue.take();
                        if (acceptData.killThread) return;

                        Socket incoming = SocketData.this.serverSocket.accept();
                        // REVIEW(mmocny): Is the above a blocking call?  Why do yoi check serverSocket for null after you call .accept on it?
                        // REVIEW(mmocny): Is this properly synchronized?
                        if (SocketData.this.serverSocket == null || SocketData.this.serverSocket.isClosed()) {
                            if (incoming != null) incoming.close();
                            // REVIEW(mmocny): context.error?
                            return;
                        }

                        SocketData sd = new SocketData(incoming);
                        int id = ChromeSocket.addSocket(sd);
                        acceptData.context.sendPluginResult(new PluginResult(PluginResult.Status.OK, id));
                    }
                } catch (InterruptedException ie) {
                    Log.w(LOG_TAG, "Thread interrupted", ie);
                    // REVIEW(mmocny): context.error? for all remaining AcceptData in queue?
                } catch (IOException ioe) {
                    if (SocketData.this.serverSocket == null || SocketData.this.serverSocket.isClosed()) {
                        Log.i(LOG_TAG, "Killing accept() thread; server socket closed.");
                    } else {
                        Log.w(LOG_TAG, "Error in accept() thread.", ioe);
                    }
                    // REVIEW(mmocny): context.error? for all remaining AcceptData in queue?
                }
            } // run()
        } // AcceptThread
    } // SocketData
}
