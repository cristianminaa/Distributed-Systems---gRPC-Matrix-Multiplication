package com.example.grpc.server.grpcserver;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import com.example.grpc.server.grpcserver.MatrixRequest;
import com.example.grpc.server.grpcserver.Matrix;
import com.example.grpc.server.grpcserver.MatrixResponse;
import com.example.grpc.server.grpcserver.MatrixServiceGrpc;
import com.example.grpc.server.grpcserver.MatrixServiceGrpc.MatrixServiceImplBase;

@GrpcService
public class MatrixServiceImpl extends MatrixServiceGrpc.MatrixServiceImplBase {

	private static final int MAX = 4;

	public static int[][] multiplyBlockArray(int A[][], int B[][]) {
		int C[][] = new int[MAX][MAX];
		C[0][0] = A[0][0] * B[0][0] + A[0][1] * B[1][0];
		C[0][1] = A[0][0] * B[0][1] + A[0][1] * B[1][1];
		C[1][0] = A[1][0] * B[0][0] + A[1][1] * B[1][0];
		C[1][1] = A[1][0] * B[0][1] + A[1][1] * B[1][1];
		return C;
	}

	public static int[][] addBlockMatrix(int A[][], int B[][]) {
		int C[][] = new int[MAX][MAX];
		for (int i = 0; i < C.length; i++) {
			for (int j = 0; j < C.length; j++) {
				C[i][j] = A[i][j] + B[i][j];
			}
		}
		return C;
	}

	public static int[][] matrixToArray(Matrix matrix) {
		int[][] temp = new int[MAX][MAX];
		temp[0][0] = matrix.getC00();
		temp[0][1] = matrix.getC01();
		temp[1][0] = matrix.getC10();
		temp[1][1] = matrix.getC11();
		return temp;
	}

	public Matrix arrayToMatrix(int[][] array) {
		Matrix C = Matrix.newBuilder()
				.setC00(array[0][0])
				.setC01(array[0][1])
				.setC10(array[1][0])
				.setC11(array[1][1])
				.build();

		return C;
	}

	@Override
	public void addBlock(MatrixRequest request, StreamObserver<MatrixResponse> reply) {
		System.out.println("Request received from client:\n" + request);
		int[][] matrixA = matrixToArray(request.getA());
		int[][] matrixB = matrixToArray(request.getB());
		int[][] newMatrix = addBlockMatrix(matrixA, matrixB);
		MatrixResponse response = MatrixResponse.newBuilder()
				.setC(arrayToMatrix(newMatrix))
				.build();
		reply.onNext(response);
		reply.onCompleted();
	}

	@Override
	public void multiplyBlock(MatrixRequest request, StreamObserver<MatrixResponse> reply) {
		System.out.println("Request received from client:\n" + request);
		int[][] matrixA = matrixToArray(request.getA());
		int[][] matrixB = matrixToArray(request.getB());
		int[][] newMatrix = multiplyBlockArray(matrixA, matrixB);
		MatrixResponse response = MatrixResponse.newBuilder()
				.setC(arrayToMatrix(newMatrix))
				.build();
		reply.onNext(response);
		reply.onCompleted();
	}
}
