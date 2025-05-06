import grpc
import sys
import os
from concurrent import futures
from grpc_reflection.v1alpha import reflection
from random import randint
sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'gen'))
from gen import dynamic_pb2_grpc, dynamic_pb2


class ExampleService(dynamic_pb2_grpc.ExampleServiceServicer):
    def UnaryCall(self, request, context):
        return dynamic_pb2.SimpleResponse(reply=f"Echo: {request.text.swapcase()}")

    def ServerStream(self, request, context):
        for i in range(1, request.count+1):
            number = i * randint(1, 10)
            yield dynamic_pb2.StreamResponse(number=number)

    def ProcessItems(self, request, context):
        modified_items = []
        for item in request.items:
            modified_name = item.name.swapcase()
            modified_id = item.id * 2
            modified_items.append(dynamic_pb2.Item(name=modified_name, id=modified_id))
        return dynamic_pb2.ComplexResponse(total=modified_items)

def serve():
    server = grpc.server(futures.ThreadPoolExecutor())
    dynamic_pb2_grpc.add_ExampleServiceServicer_to_server(ExampleService(), server)

    SERVICE_NAMES = (
        dynamic_pb2.DESCRIPTOR.services_by_name['ExampleService'].full_name,
        reflection.SERVICE_NAME,
    )
    reflection.enable_server_reflection(SERVICE_NAMES, server)

    server.add_insecure_port('[::]:50051')
    server.start()
    server.wait_for_termination()

if __name__ == '__main__':
    serve()
