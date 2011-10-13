/*
 * Copyright 2011 qorron
 * Contact: https://github.com/qorron
 * 
 * This file is part of 666up!
 * 
 * 666up! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * 666up! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with 666up!.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.qless.up666;

import java.io.DataOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

/**
 * 
 * @author quattro
 * 
 *         resizes an image if it is too large
 * 
 */

public class ImageProcessor {

	/**
	 * maximum pixels in one dimension
	 */
	static int maxPixel = 1200;

	/**
	 * loads, resizes and writes the image to the http connection
	 * 
	 * @param imagePath
	 *            the path to the image file
	 * @param httpPostOutputStream
	 *            the http stream
	 */
	public static void process(String imagePath,
			DataOutputStream httpPostOutputStream) {

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true; // to get the size without actually
											// loading the image into the
											// memory.
		Bitmap bitmapOrg = BitmapFactory.decodeFile(imagePath, options);

		int originalHeight = options.outHeight;
		int originalWidth = options.outWidth;

		int originalPixels = originalHeight * originalWidth;
		// Note: change this to long once gigapixel cameras have arrived

		int maxPixels = maxPixel * maxPixel;
		int maxPixelsForLoading = maxPixels; // multiply by 4 to leave some
												// room and scale down the
												// rest in memory

		double baseInSampleSize = 1;
		Log.i("ImageScale", "maxPixelsForLoading: " + maxPixelsForLoading
				+ " originalPixels: " + originalPixels + " baseInSampleSize: "
				+ baseInSampleSize);

		for (; originalPixels > maxPixelsForLoading; originalPixels /= 4) {
			baseInSampleSize *= 2;
		}

		// outMimeType

		options.inJustDecodeBounds = false;
		options.inSampleSize = (int) baseInSampleSize;

		Log.i("ImageScale", "maxPixelsForLoading: " + maxPixelsForLoading
				+ " originalPixels: " + originalPixels + " baseInSampleSize: "
				+ baseInSampleSize);

		bitmapOrg = BitmapFactory.decodeFile(imagePath, options);

		int width = bitmapOrg.getWidth();
		int height = bitmapOrg.getHeight();

		// calculate the scale
		float scaleWidth = 1;
		float scaleHeight = 1;
		scaleWidth = scaleHeight = ((float) maxPixels) / originalPixels;

		Log.i("ImageScale", "width: " + width + " height: " + height
				+ "scale: " + scaleWidth);

		// create a matrix for the manipulation
		Matrix matrix = new Matrix();
		// resize the bit map
		matrix.postScale(scaleWidth, scaleHeight);
		// rotate the Bitmap
		// matrix.postRotate(45);

		bitmapOrg
				.compress(Bitmap.CompressFormat.JPEG, 90, httpPostOutputStream);

		// recreate the new Bitmap
		// Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width,
		// height, matrix, true);
		//
		// resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90,
		// httpPostOutputStream);

	}
}
