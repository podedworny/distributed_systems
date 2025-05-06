package sr.grpc.client;

import com.google.protobuf.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.StreamObserver;


import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.grpc.*;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.stub.ClientCalls;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.ByteString;


public class grpcClient
{

	private final ManagedChannel channel;
	private final ServerReflectionGrpc.ServerReflectionStub reflectionStub;
	private final Map<String, FileDescriptor> fileDescriptors = new HashMap<>();
	private final Map<String, Descriptors.ServiceDescriptor> serviceDescriptors = new HashMap<>();
	private final Map<String, Descriptors.MethodDescriptor> methodDescriptors = new HashMap<>();


	public grpcClient(String remoteHost, int remotePort)
	{
		channel = ManagedChannelBuilder
				.forAddress(remoteHost, remotePort)
				.usePlaintext()
				.build();

		reflectionStub = ServerReflectionGrpc.newStub(channel);
	}


	public void discoverServiceMethods(String serviceName) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		final List<FileDescriptorProto> fileDescriptorProtos = new ArrayList<>();

		StreamObserver<ServerReflectionResponse> responseObserver = new StreamObserver<>() {
			@Override
			public void onNext(ServerReflectionResponse response) {
				if (response.hasFileDescriptorResponse()) {
					List<ByteString> fileDescriptors = response.getFileDescriptorResponse().getFileDescriptorProtoList();
					for (ByteString fdBytes : fileDescriptors) {
						try {
							FileDescriptorProto fdProto = FileDescriptorProto.parseFrom(fdBytes);
							fileDescriptorProtos.add(fdProto);
						} catch (Exception e) {
							System.err.println("Error parsing file descriptor: " + e.getMessage());
						}
					}
				}
			}

			@Override
			public void onError(Throwable t) {
				System.err.println("Server reflection error: " + t);
				latch.countDown();
			}

			@Override
			public void onCompleted() {
				System.out.println("--List of functions--");
				System.out.println("-> unaryCall {text}");
				System.out.println("-> serverStream {count}");
				System.out.println("-> processItems {count}");
				latch.countDown();
			}
		};

		StreamObserver<ServerReflectionRequest> requestObserver = reflectionStub.serverReflectionInfo(responseObserver);
		requestObserver.onNext(ServerReflectionRequest.newBuilder()
				.setFileContainingSymbol(serviceName)
				.build());
		requestObserver.onCompleted();

		latch.await();

		Map<String, FileDescriptorProto> fileDescriptorProtoMap = new HashMap<>();
		for (FileDescriptorProto proto : fileDescriptorProtos) {
			fileDescriptorProtoMap.put(proto.getName(), proto);
		}

		for (FileDescriptorProto proto : fileDescriptorProtos) {
			try {
				FileDescriptor fd = buildFileDescriptor(proto, fileDescriptorProtoMap, new HashMap<>());
				fileDescriptors.put(proto.getName(), fd);

                assert fd != null;
                for (Descriptors.ServiceDescriptor service : fd.getServices()) {
					serviceDescriptors.put(service.getFullName(), service);

					for (Descriptors.MethodDescriptor method : service.getMethods())
						methodDescriptors.put(method.getName(), method);

				}
			} catch (Exception e) {
				System.err.println("Failed to process file descriptor: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private FileDescriptor buildFileDescriptor(
			FileDescriptorProto proto,
			Map<String, FileDescriptorProto> fileDescriptorProtoMap,
			Map<String, FileDescriptor> builtDescriptors) {

		String name = proto.getName();
		if (builtDescriptors.containsKey(name)) {
			return builtDescriptors.get(name);
		}

		FileDescriptor[] dependencies = new FileDescriptor[proto.getDependencyCount()];
		for (int i = 0; i < proto.getDependencyCount(); i++) {
			String dependency = proto.getDependency(i);
			FileDescriptorProto dependencyProto = fileDescriptorProtoMap.get(dependency);
			if (dependencyProto != null) {
				dependencies[i] = buildFileDescriptor(dependencyProto, fileDescriptorProtoMap, builtDescriptors);
			} else {
				System.err.println("Missing dependency: " + dependency);
			}
		}

		try {
			FileDescriptor fileDescriptor = FileDescriptor.buildFrom(proto, dependencies);
			builtDescriptors.put(name, fileDescriptor);
			return fileDescriptor;
		} catch (Descriptors.DescriptorValidationException e) {
			System.err.println("Failed to build descriptor " + name + ": " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public static void main(String[] args) throws Exception
	{
		grpcClient client = new grpcClient("127.0.0.5", 50051);
		client.discoverServiceMethods("dynamic.ExampleService");
		client.test();
	}

	public void shutdown() throws InterruptedException {
		channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}


	public void test() throws InterruptedException
	{
		String line = null;
		java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));

		do 	{
			try	{
				System.out.print("==> ");
				System.out.flush();
				line = in.readLine();
				String[] parts = line.split(" ", 2);
				String methodName = parts[0];
				String params = parts.length > 1 ? parts[1] : "";
				switch (methodName) {
					case "unaryCall": {
						Descriptors.MethodDescriptor methodDesc = methodDescriptors.get("UnaryCall");

						if (methodDesc == null) {
							System.err.println("Method UnaryCall not found.");
							break;
						}

						Descriptors.Descriptor inputType = methodDesc.getInputType();
						Descriptors.Descriptor outputType = methodDesc.getOutputType();

						Descriptors.FieldDescriptor textField = inputType.findFieldByName("text");
						if (textField == null) {
							System.err.println("Field 'text' not found in " + inputType.getFullName());
							for (Descriptors.FieldDescriptor field : inputType.getFields()) {
								System.out.println("Available field: " + field.getName());
							}
							break;
						}

						if (params.isEmpty())
							params = "Hello, World!";

						DynamicMessage requestMessage = DynamicMessage.newBuilder(inputType)
								.setField(textField, params)
								.build();

						MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor =
								MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
										.setType(MethodDescriptor.MethodType.UNARY)
										.setFullMethodName("dynamic.ExampleService/UnaryCall")
										.setRequestMarshaller(ProtoUtils.marshaller(DynamicMessage.newBuilder(inputType).build()))
										.setResponseMarshaller(ProtoUtils.marshaller(DynamicMessage.newBuilder(outputType).build()))
										.build();

						ClientCall<DynamicMessage, DynamicMessage> call = channel.newCall(methodDescriptor, CallOptions.DEFAULT);
						DynamicMessage response = ClientCalls.blockingUnaryCall(call, requestMessage);

						Descriptors.FieldDescriptor replyField = outputType.findFieldByName("reply");
						System.out.println("Response: " + response.getField(replyField));
						break;
					}
					case "serverStream": {
						Descriptors.MethodDescriptor methodDesc = methodDescriptors.get("ServerStream");

						if (methodDesc == null) {
							System.err.println("Method ServerStream not found.");
							break;
						}

						Descriptors.Descriptor inputType = methodDesc.getInputType();
						Descriptors.Descriptor outputType = methodDesc.getOutputType();

						Descriptors.FieldDescriptor countField = inputType.findFieldByName("count");
						if (countField == null) {
							System.err.println("Field 'count' not found in " + inputType.getFullName());
							for (Descriptors.FieldDescriptor field : inputType.getFields())
								System.out.println("Available field: " + field.getName());
							break;
						}

						int count;
						try {
							count = Integer.parseInt(params);
						} catch (NumberFormatException e) {
							count = 5;
						}

						DynamicMessage requestMessage = DynamicMessage.newBuilder(inputType)
								.setField(countField, count)
								.build();

						MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor =
								MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
										.setType(MethodDescriptor.MethodType.SERVER_STREAMING)
										.setFullMethodName("dynamic.ExampleService/ServerStream")
										.setRequestMarshaller(ProtoUtils.marshaller(DynamicMessage.newBuilder(inputType).build()))
										.setResponseMarshaller(ProtoUtils.marshaller(DynamicMessage.newBuilder(outputType).build()))
										.build();

						ClientCall<DynamicMessage, DynamicMessage> call = channel.newCall(methodDescriptor, CallOptions.DEFAULT);
						final CountDownLatch streamLatch = new CountDownLatch(1);
						final List<String> responses = new ArrayList<>();

						ClientCalls.asyncServerStreamingCall(call, requestMessage,
								new StreamObserver<>() {
									@Override
									public void onNext(DynamicMessage value) {
										responses.add(value.toString());
									}

									@Override
									public void onError(Throwable t) {
										System.err.println("Stream error: " + t.getMessage());
										streamLatch.countDown();
									}

									@Override
									public void onCompleted() {
										for(String response : responses) {
											System.out.println("Response: " + response);
										}
										streamLatch.countDown();
									}
								});


						streamLatch.await();
						break;
					}

					case "processItems": {
						Descriptors.MethodDescriptor methodDesc = methodDescriptors.get("ProcessItems");

						if (methodDesc == null) {
							System.err.println("Method ProcessItems not found.");
							break;
						}

						Descriptors.Descriptor inputType = methodDesc.getInputType();
						Descriptors.Descriptor outputType = methodDesc.getOutputType();

						Descriptors.FieldDescriptor itemsField = inputType.findFieldByName("items");
						if (itemsField == null) {
							System.err.println("Field 'items' not found in " + inputType.getFullName());
							for (Descriptors.FieldDescriptor field : inputType.getFields())
								System.out.println("Available field: " + field.getName());
							break;
						}

						Descriptors.Descriptor itemType = itemsField.getMessageType();

						DynamicMessage.Builder requestBuilder = DynamicMessage.newBuilder(inputType);

						int count;
						try {
							count = Integer.parseInt(params);
						} catch (NumberFormatException e) {
							count = 5;
						}

						List<DynamicMessage> items = new ArrayList<>();
						for (int i = 1; i <= count; i++) {
							DynamicMessage item = DynamicMessage.newBuilder(itemType)
									.setField(itemType.findFieldByName("id"), i)
									.setField(itemType.findFieldByName("name"), "Item " + (char)(i + 64))
									.build();
							items.add(item);
						}

						requestBuilder.setField(itemsField, items);
						DynamicMessage requestMessage = requestBuilder.build();

						MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor =
								MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
										.setType(MethodDescriptor.MethodType.UNARY)
										.setFullMethodName("dynamic.ExampleService/ProcessItems")
										.setRequestMarshaller(ProtoUtils.marshaller(DynamicMessage.newBuilder(inputType).build()))
										.setResponseMarshaller(ProtoUtils.marshaller(DynamicMessage.newBuilder(outputType).build()))
										.build();

						ClientCall<DynamicMessage, DynamicMessage> call = channel.newCall(methodDescriptor, CallOptions.DEFAULT);
						DynamicMessage response = ClientCalls.blockingUnaryCall(call, requestMessage);
						Descriptors.FieldDescriptor totalFd = outputType.findFieldByName("total");

						@SuppressWarnings("unchecked")
						List<DynamicMessage> itemsList = (List<DynamicMessage>) response.getField(totalFd);

						Descriptors.FieldDescriptor idFd   = itemType.findFieldByName("id");
						Descriptors.FieldDescriptor nameFd = itemType.findFieldByName("name");

						System.out.print("Response: [ ");
						for (DynamicMessage itemMsg : itemsList) {
							System.out.print("(" + itemMsg.getField(idFd) + ", " + itemMsg.getField(nameFd) + ") ");
						}
						System.out.println("]");
						break;
					}

					case "x":
					case "":
						break;
					default:
						System.out.println("???");
						break;
				}
			}
			catch (java.io.IOException ex) {
				System.err.println(ex);
			}
        }
		while (!line.equals("x"));

		shutdown();
	}
}
