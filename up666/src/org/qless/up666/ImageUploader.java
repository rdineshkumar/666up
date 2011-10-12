/**
 * 
 */
package org.qless.up666;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

/**
 * @author quattro
 * 
 * uploads the image and calls the {@link ImageProcessor} should the file be to large
 * 
 */
public class ImageUploader {

	/**
	 * handles the image upload
	 * 
	 * @param filename the path to the image
	 * @return the {@link URL} of the uploaded image or null if the upload failed
	 * @throws MalformedURLException if the upload failed
	 * @throws IOException on a network error
	 * @throws ProtocolException the server responded unexpectedly
	 * @throws FileNotFoundException the suggested file was not found
	 */
	public static URL upload(String filename) throws MalformedURLException, IOException, ProtocolException, FileNotFoundException {
		HttpURLConnection connection = null;
		DataOutputStream outputStream = null;

		String pathToOurFile = filename;
		String urlServer = "http://666kb.com/u.php";
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";

		final long maxSize = 681984;

		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1 * 1024 * 1024;

		URL imageURL = null;


			URL url = new URL(urlServer); // theoretically possible MalformedURLException 
			connection = (HttpURLConnection) url.openConnection(); // IOException -> network error

			// Allow Inputs & Outputs
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);

			// Enable POST method
			connection.setRequestMethod("POST"); // ProtocolException -> 

			connection.setRequestProperty("Connection", "Keep-Alive");
			connection.setRequestProperty("Content-Type",
					"multipart/form-data;boundary=" + boundary);

			outputStream = new DataOutputStream(connection.getOutputStream()); // IOException -> network error

			outputStream.writeBytes(twoHyphens + boundary + lineEnd); // IOException -> network error
			outputStream
					.writeBytes("Content-Disposition: form-data; name=\"MAX_FILE_SIZE\""
							+ lineEnd);
			outputStream.writeBytes(lineEnd); // IOException -> network error
			outputStream.writeBytes(maxSize + lineEnd); // IOException -> network error

			outputStream.writeBytes(twoHyphens + boundary + lineEnd); // ...
			outputStream
					.writeBytes("Content-Disposition: form-data; name=\"submit\""
							+ lineEnd);
			outputStream.writeBytes(lineEnd);
			outputStream.writeBytes(" Speichern " + lineEnd);

			outputStream.writeBytes(twoHyphens + boundary + lineEnd);
			outputStream
					.writeBytes("Content-Disposition: form-data; name=\"f\"; filename=\""
							+ pathToOurFile + "\"" + lineEnd);
			outputStream.writeBytes(lineEnd);

			File file = new File(filename);

			// Get the number of bytes in the file
			long length = file.length();

			if (length > maxSize) {
				ImageProcessor.process(filename, outputStream);
			} else {
				FileInputStream fileInputStream = new FileInputStream(new File(
						pathToOurFile));  // FileNotFoundException -> most likely the filename got escaped anf needs to be unescaped (known to happen on froyo+ and sending from a filemanager)

				bytesAvailable = fileInputStream.available(); // IOException (file) -> should not really happen unless sdcard is broken?
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				buffer = new byte[bufferSize];

				// Read file
				bytesRead = fileInputStream.read(buffer, 0, bufferSize); // IOException (file)

				while (bytesRead > 0) {
					outputStream.write(buffer, 0, bufferSize); // IOException -> network error
					bytesAvailable = fileInputStream.available(); // IOException (file)
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					bytesRead = fileInputStream.read(buffer, 0, bufferSize); // IOException (file) 
				}
				fileInputStream.close(); // IOException (file) 

			}
			outputStream.writeBytes(lineEnd); // IOException -> network error
			outputStream.writeBytes(twoHyphens + boundary + twoHyphens
					+ lineEnd);

			outputStream.flush(); // IOException -> network error
			outputStream.close(); // ..

			// Responses from the server (code and message)
			int serverResponseCode = connection.getResponseCode(); // IOException -> network error
			String serverResponseMessage = connection.getResponseMessage(); // IOException -> network error

			Log.i("ImageUpload", serverResponseCode + serverResponseMessage);
			InputStream in = new BufferedInputStream(
					connection.getInputStream()); // IOException -> network error

			Scanner sc = new Scanner(in);
			String content = sc.useDelimiter("\\Z").next();
			sc.close();

			imageURL = new URL(parseResultPage(content)); // MalformedURLException -> if parseResultPage(String page) fails

			Log.d("ImageUpload", imageURL.toString());
		return imageURL;
	}

	/**
	 * Tkes the whole webpage as a String and tries to parse the image url
	 * 
	 * @param page the HTML document
	 * @return the URL of the uploaded image or null if it was not found
	 */
	private static String parseResultPage(String page) {
		Pattern p = Pattern.compile("(\\Qhttp://666kb.com/i/\\E.*?)\"");
		Matcher m = p.matcher(page); // get a matcher object
		if (m.find()) {
			return m.group(1);
		} else {
			Log.d("content", page);
			return null;
		}
	}
}