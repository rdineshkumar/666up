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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
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
	 * generates a thumbnail of the image and returns it as byte[]
	 * 
	 * @param imagePath
	 *            the path to the image file
	 * @param size
	 *            maximum pixels in one dimension
	 * 
	 */
	public static byte[] thumbnail(String imagePath, int size) {
		int bytes = size * size / 2;
		ByteArrayOutputStream thumbnailStream = new ByteArrayOutputStream(bytes);
		process(imagePath, thumbnailStream, size, true);

		Log.i("ImageScale", "thumbnail requested size: " + size + " projected bytes: " + bytes
				+ " used bytes: " + thumbnailStream.size());

		return thumbnailStream.toByteArray();
	}

	/**
	 * loads, resizes and returns the image as Bitmap.
	 * 
	 * @param imagePath
	 *            the path to the image file
	 * @return the scaled down image
	 */
	public static Bitmap process(String imagePath) {
		return process(imagePath, maxPixel, false);
	}

	/**
	 * loads, resizes and returns the image as Bitmap.
	 * 
	 * @param imagePath
	 *            the path to the image file
	 * @param maxDimension
	 *            maximum pixels in one direction
	 * @param strict
	 *            set to true if the resulting image should have exactly this dimension
	 * @return the scaled down image
	 */
	public static Bitmap process(String imagePath, int maxDimension, boolean strict) {

		Log.d("ImageScale", "check rotation");
		int rotate = 0;
		try {
			ExifInterface exif = new ExifInterface(imagePath);
			switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL)) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				rotate = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				rotate = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				rotate = 270;
				break;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Log.d("ImageScale", "check dimensions");
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true; // to get the size without actually
											// loading the image into the
											// memory.
		Bitmap bitmapOrg = BitmapFactory.decodeFile(imagePath, options);

		int originalHeight = options.outHeight;
		int originalWidth = options.outWidth;

		int originalPixels = originalHeight * originalWidth;
		// Note: change this to long once gigapixel cameras have arrived

		int maxPixels = maxDimension * maxDimension;
		int maxPixelsForLoading = maxPixels;

		if (strict) {
			maxPixelsForLoading *= 4;
			// multiply by 4 to leave some
			// room and scale down to the
			// exact size in memory
		}

		double baseInSampleSize = 1;
		Log.i("ImageScale", "maxPixelsForLoading: " + maxPixelsForLoading + " originalPixels: "
				+ originalPixels + " baseInSampleSize: " + baseInSampleSize + " first");

		for (; originalPixels > maxPixelsForLoading; originalPixels /= 4) {
			baseInSampleSize *= 2;
		}

		// outMimeType

		options.inJustDecodeBounds = false;
		options.inSampleSize = (int) baseInSampleSize;

		Log.i("ImageScale", "maxPixelsForLoading: " + maxPixelsForLoading + " originalPixels: "
				+ originalPixels + " baseInSampleSize: " + baseInSampleSize + " final");

		bitmapOrg = BitmapFactory.decodeFile(imagePath, options);
		Log.d("ImageScale", "Bitmap decoded");
		if (strict || rotate != 0) {
			int width = bitmapOrg.getWidth();
			int height = bitmapOrg.getHeight();

			int largestDimension = 0;
			if (width > height) {
				largestDimension = width;
			} else {
				largestDimension = height;
			}

			// calculate the scale
			float scaleWidth = 1;
			float scaleHeight = 1;
			scaleWidth = scaleHeight = ((float) maxDimension) / largestDimension;

			Log.i("ImageScale", "width: " + width + " height: " + height + "scale: " + scaleWidth);

			// create a matrix for the manipulation
			Matrix matrix = new Matrix();
			// resize the bit map
			matrix.postScale(scaleWidth, scaleHeight);
			// rotate the Bitmap
			matrix.postRotate(rotate);
			Log.i("ImageScale", "rotate: " + rotate);

			// recreate the new Bitmap
			Bitmap resizedBitmap = Bitmap
					.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true);
			Log.d("ImageScale", "Bitmap created");
			Log.i("ImageScale", "new image width: " + resizedBitmap.getHeight() + " height: "
					+ resizedBitmap.getWidth());
			return resizedBitmap;
		} else {
			Log.d("ImageScale", "Bitmap fast rescale");
			return bitmapOrg;
		}
	}

	/**
	 * loads, resizes and writes the image to the http connection
	 * 
	 * @param imagePath
	 *            the path to the image file
	 * @param imageOutputStream
	 *            the http stream
	 */
	public static void process(String imagePath, OutputStream imageOutputStream) {
		process(imagePath, imageOutputStream, maxPixel, false);
	}

	/**
	 * loads, resizes and writes the image to the http connection
	 * 
	 * @param imagePath
	 *            the path to the image file
	 * @param imageOutputStream
	 *            the http stream
	 * @param maxDimension
	 *            maximum pixels in one direction
	 * @param strict
	 *            set to true if the resulting image should have exactly this dimension
	 * 
	 */
	public static void process(String imagePath, OutputStream imageOutputStream, int maxDimension,
			boolean strict) {
		process(imagePath, maxDimension, strict).compress(Bitmap.CompressFormat.JPEG, 90,
				imageOutputStream);
	}
}
