import org.opencv.core.Core;
import org.opencv.core.Mat;

/**
 * Class contains static methods for image interpolation
 * @author michal
 *
 */
public final class Interpolation {
	public static final int MAGNITUDE_DOUBLE = 2;
	public static final int MAGNITUDE_QUADRUPLE = 4;
	
	/**
	 * Bilinear interpolation
	 * @param image Source image
	 * @param magnitude Magnitude of interpolation
	 * @return Interpolated image
	 */
	public static Mat bilinear(Mat image, int magnitude){
		if (image == null)
			return null;
		if(magnitude < 1)
			return null;
		
		int new_h = (image.height()-1) * magnitude + 1;
		int new_w = (image.width()-1) * magnitude + 1;
		
		Mat new_image = new Mat(new_h, new_w, image.type());		

		//set pixels from old image to new image
		for (int y = 0; y < image.height(); ++y){//for pixels in old image
			for (int x = 0; x < image.width(); ++x){ 
				
				double[] a, b, c, d;
				
				//get known pixels from image
				/*****************************/
				a = image.get(y, x);
				b = image.get(y,  x + 1); 
				c = image.get(y + 1,  x);
				d = image.get(y + 1,  x + 1);
				/********************************/
				double[] pix = new double[image.channels()];
				for (int k = 0; k < magnitude; ++k){ //for subpixels created from a pixel
					if(b != null){ //check if we aren't on the edge of image
						for (int l = 0; l < magnitude; ++l){ 
							double w = l/magnitude;
							double h = k/magnitude;
							if (c != null){ // check if we aren't on the edge of image
								for (int i = 0; i < image.channels(); ++i){ //for all colors
									pix[i] = a[i] + w*(b[i] - a[i]) +  h*(c[i] + w*(d[i] - c[i]) - w*(b[i] - a[i]));
								}
							}
							else{
								for (int i = 0; i < image.channels(); ++i){ //for all colors
									pix[i] = a[i] + w*(b[i] - a[i]);
								}
							}
							new_image.put(y*magnitude + k,  x*magnitude + l,  pix);
						}
					}
					else{
						double h = k/magnitude;
						if(c != null){
							for (int i = 0; i < image.channels(); ++i){ //for all colors
								pix[i] = a[i] +  h*(c[i]);
							}
						}
						new_image.put(y*magnitude + k,  x*magnitude,  pix);
						//new_image.put(y*magnitude,  x*magnitude, a);
					}
				}
			}	
		}
		return new_image;
	}
	
	/**
	 * SOI filter based interpolation from h.264 standard
	 * @param image Source image
	 * @param magnitude Magnitude of interpolation. Accepts only {@link MAGNITUDE_DOUBLE} or {@link MAGNITUDE_QUADRUPLE}
	 * @return Interpolated image
	 */
	public static Mat h264(Mat image, int magnitude){
		final int OFFSET = 2;
		if (image == null)
			return null;
		if(magnitude != MAGNITUDE_QUADRUPLE)
			if(magnitude != MAGNITUDE_DOUBLE)
				return null;
		
		int new_h = (image.height()-1) * magnitude + 1;
		int new_w = (image.width()-1) * magnitude + 1;
		
		
		Mat new_image = new Mat(new_h, new_w, image.type());
		
		Mat extended_image = new Mat(image.height() + 2*OFFSET, image.width() + 2*OFFSET, image.type());
		Mat new_extended_image = new Mat(extended_image.height()*magnitude, extended_image.width()*magnitude, image.type());
		
		Core.copyMakeBorder(image, extended_image, OFFSET, OFFSET, OFFSET, OFFSET, Core.BORDER_REPLICATE); //extend image
		//if(true) return extended_image;
		
		for (int y = OFFSET; y < image.height() + OFFSET; ++y){ // filter horizontally //2 offset because of border
			for (int x = OFFSET; x < image.width() + OFFSET - 1; ++x){ //don't write last column of pixels so there is no condition inside loop, we will write it later
				double[] E = extended_image.get(y,  x - 2);
				double[] F = extended_image.get(y,  x - 1);
				double[] G = extended_image.get(y, x);
				double[] H = extended_image.get(y,  x + 1);
				double[] I = extended_image.get(y,  x + 2);
				double[] J = extended_image.get(y,  x + 3);
				
				double[] pix = new double[extended_image.channels()];
				for(int z = 0; z < extended_image.channels(); ++z){
					pix[z] = (E[z] - 5*F[z] + 20*G[z] + 20*H[z] - 5*I[z] + J[z] + 16) / 32;
				}
				int x_new = (x - OFFSET)*magnitude;
				int y_new = (y - OFFSET)*magnitude;
				new_image.put(y_new, x_new, G);
				new_image.put(y_new, x_new + magnitude/2, pix);
			}
		}
		for(int y = 0; y < image.height(); ++y){ //write last column
			new_image.put(y*magnitude, new_image.width() - 1, image.get(y,  image.width() - 1));
		}
		
		for (int x = OFFSET; x < image.width() + OFFSET; ++x){ // filter vertically //2 offset because of border
			for (int y = OFFSET; y < image.height() + OFFSET - 1; ++y){ // don't write last row
				double[] A = extended_image.get(y - 2, x);
				double[] C = extended_image.get(y - 1, x);
				double[] G = extended_image.get(y,  x);
				double[] M = extended_image.get(y + 1, x);
				double[] R = extended_image.get(y + 2, x);
				double[] T = extended_image.get(y + 3, x);
				
				double[] pix = new double[extended_image.channels()];
				for(int z = 0; z < extended_image.channels(); ++z){
					pix[z] = (A[z] - 5*C[z] + 20*G[z] + 20*M[z] - 5*R[z] + T[z] + 16) / 32;
				}
				int x_new = (x - OFFSET)*magnitude;
				int y_new = (y - OFFSET)*magnitude;
				new_image.put(y_new, x_new, G);
				new_image.put(y_new + magnitude/2, x_new, pix);
			}
		}
		for(int x = 0; x < image.width(); ++x){ //write last row
			new_image.put(new_image.height() - 1, x*magnitude, image.get(image.height() - 1, x));
		}
		
		int extended_offset = OFFSET*magnitude;
		Core.copyMakeBorder(new_image, new_extended_image, extended_offset, extended_offset, extended_offset, extended_offset, Core.BORDER_REPLICATE);
		//if(true) return new_extended_image;
		
		for (int y = OFFSET; y < image.height() + OFFSET - 1; ++y){ //set value of center point
			for (int x = OFFSET; x < image.width() + OFFSET - 1; ++x){
				int maxx = image.width() + OFFSET;
				int maxn = new_extended_image.width();
				double[] aa = new_extended_image.get((y - 2)*magnitude,  x*magnitude + magnitude/2);
				double[] bb = new_extended_image.get((y - 1)*magnitude, x*magnitude + magnitude/2);
				double[] b = new_extended_image.get(y*magnitude, x*magnitude + magnitude/2);
				double[] s = new_extended_image.get((y + 1)*magnitude, x*magnitude + magnitude/2);
				double[] gg = new_extended_image.get((y + 2)*magnitude, x*magnitude + magnitude/2);
				double[] hh = new_extended_image.get((y + 3)*magnitude, x*magnitude + magnitude/2);
				/*
				double[] cc = new_extended_image.get(y*magnitude + magnitude/2, (x - 2)*magnitude);
				double[] dd = new_extended_image.get(y*magnitude + magnitude/2, (x - 1)*magnitude);
				double[] h = new_extended_image.get(y*magnitude + magnitude/2, x*magnitude);
				double[] m = new_extended_image.get(y*magnitude + magnitude/2, (x + 1)*magnitude);
				double[] ee = new_extended_image.get(y*magnitude + magnitude/2, (x + 2)*magnitude);
				double[] ff = new_extended_image.get(y*magnitude + magnitude/2, (x + 3)*magnitude);
				*/
				double[] pix = new double[extended_image.channels()];
				for(int z = 0; z < extended_image.channels(); ++z){
					pix[z] = (aa[z] - 5*bb[z] + 20*b[z] + 20*s[z] - 5*gg[z] + hh[z] + 16) / 32;
				}
				new_image.put((y - OFFSET)*magnitude + magnitude/2, (x - OFFSET)*magnitude + magnitude/2, pix);
			}
		}
		
		if(magnitude == MAGNITUDE_DOUBLE) //return image if double upscaling was selected
			return new_image;
		
		//QUADRUPLE magnitude
		
		for(int y = 0; y < image.height() - 1; ++y){ //leave alone last row and column, we will do it later
			for(int x = 0; x < image.width() - 1; ++x){
				double[] G = new_image.get(y*magnitude, x*magnitude);
				double[] b = new_image.get(y*magnitude, x*magnitude + 2);
				double[] H = new_image.get(y*magnitude, (x + 1)*magnitude);
				double[] h = new_image.get(y*magnitude + 2, x*magnitude);
				double[] j = new_image.get(y*magnitude + 2, x*magnitude + 2);
				double[] m = new_image.get(y*magnitude + 2, (x + 1) * magnitude);
				double[] M = new_image.get((y + 1)*magnitude, x*magnitude);
				double[] s = new_image.get((y + 1)*magnitude, x*magnitude + 2);
				//double[] N = new_image.get((y + 1)*magnitude, (x + 1)*magnitude);
				
				double[] pix = new double[extended_image.channels()];
				for(int z = 0; z < extended_image.channels(); ++z) //a
					pix[z] = (G[z] + b[z] + 1)/2;
				new_image.put(y*magnitude, x*magnitude + 1, pix);
				
				for(int z = 0; z < extended_image.channels(); ++z) //c
					pix[z] = (H[z] + b[z] + 1)/2;
				new_image.put(y*magnitude, x*magnitude + 3, pix);
				
				for(int z = 0; z < extended_image.channels(); ++z) //d
					pix[z] = (G[z] +h[z] + 1)/2;
				new_image.put(y*magnitude + 1, x*magnitude, pix);
				
				for(int z = 0; z < extended_image.channels(); ++z) //e
					pix[z] = (b[z] + h[z] + 1)/2;
				new_image.put(y*magnitude + 1, x*magnitude + 1, pix);
				
				for(int z = 0; z < extended_image.channels(); ++z) //f
					pix[z] = (b[z] + j[z] + 1)/2;
				new_image.put(y*magnitude + 1, x*magnitude + 2, pix);
				
				for(int z = 0; z < extended_image.channels(); ++z) //g
					pix[z] = (b[z] + m[z] + 1)/2;
				new_image.put(y*magnitude + 1, x*magnitude + 3, pix);
				
				for(int z = 0; z < extended_image.channels(); ++z) //i
					pix[z] = (h[z] + j[z] + 1)/2;
				new_image.put(y*magnitude + 2, x*magnitude + 1, pix);
				
				for(int z = 0; z < extended_image.channels(); ++z) //k
					pix[z] = (j[z] + m[z] + 1)/2;
				new_image.put(y*magnitude + 2, x*magnitude + 3, pix);
				
				for(int z = 0; z < extended_image.channels(); ++z) //n
					pix[z] = (M[z] + h[z] + 1)/2;
				new_image.put(y*magnitude + 3, x*magnitude, pix);
				
				for(int z = 0; z < extended_image.channels(); ++z) //p
					pix[z] = (h[z] + s[z] + 1)/2;
				new_image.put(y*magnitude + 3, x*magnitude + 1, pix);
				
				for(int z = 0; z < extended_image.channels(); ++z) //q
					pix[z] = (j[z] + s[z] + 1)/2;
				new_image.put(y*magnitude + 3, x*magnitude + 2, pix);
				
				for(int z = 0; z < extended_image.channels(); ++z) //r
					pix[z] = (m[z] + s[z] + 1)/2;
				new_image.put(y*magnitude + 3, x*magnitude + 3, pix);
			}
		}
		
		for(int x = 0; x < image.width() - 1; ++x){ //write last row
			double[] G = new_image.get(new_image.height() - 1, x*magnitude);
			double[] b = new_image.get(new_image.height() - 1, x*magnitude + 2);
			double[] H = new_image.get(new_image.height() - 1, (x + 1)*magnitude);
			
			double[] pix = new double[extended_image.channels()];
			for(int z = 0; z < extended_image.channels(); ++z) //a
				pix[z] = (G[z] + b[z] + 1)/2;
			new_image.put(new_image.height() - 1, x*magnitude + 1, pix);
			
			for(int z = 0; z < extended_image.channels(); ++z) //c
				pix[z] = (H[z] + b[z] + 1)/2;
			new_image.put(new_image.height() - 1, x*magnitude + 3, pix);
		}
		
		for(int y = 0; y < image.height() - 1; ++y){ //write last column
			double[] G = new_image.get(y*magnitude, new_image.width() - 1);
			double[] h = new_image.get(y*magnitude + 2, new_image.width() - 1);
			double[] M = new_image.get((y + 1)*magnitude, new_image.width() - 1);
			
			double[] pix = new double[extended_image.channels()];
			for(int z = 0; z < extended_image.channels(); ++z) //d
				pix[z] = (G[z] + h[z] + 1)/2;
			new_image.put(y*magnitude + 1, new_image.width() - 1, pix);
			
			for(int z = 0; z < extended_image.channels(); ++z) //n
				pix[z] = (M[z] + h[z] + 1)/2;
			new_image.put(y*magnitude + 3, new_image.width() - 1, pix);
		}
		
		return new_image;
	}
	
	private Interpolation() {
	}
}
