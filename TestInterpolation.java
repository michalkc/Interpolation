import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

public class TestInterpolation {

	public static void main(String[] args) {
		System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
		Mat source = Imgcodecs.imread("D:\\folder.jpg");
		Mat output = Interpolation.bilinear(source,  Interpolation.MAGNITUDE_QUADRUPLE);
		Imgcodecs.imwrite("D:\\folderb.jpg", output);
		output = Interpolation.h264(source,  Interpolation.MAGNITUDE_QUADRUPLE);
		Imgcodecs.imwrite("D:\\folderh.jpg", output);
	}

}
