package com.gorcer.iseeyou.service;

import com.gorcer.iseeyou.model.CarNumberResponse;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_core.*;
import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Vector;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvSaveImage;
import static org.bytedeco.javacpp.opencv_imgproc.*;

@Service
public class Anrp {

    static double angle(CvPoint pt1, CvPoint pt2, CvPoint pt0) {
        double dx1 = pt1.x() - pt0.x();
        double dy1 = pt1.y() - pt0.y();
        double dx2 = pt2.x() - pt0.x();
        double dy2 = pt2.y() - pt0.y();
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }


    public static Vector<CvSeq> findSquares(IplImage src, CvMemStorage storage) {
        int N = 10;
        int thresh = 100;

        float val_thresh = 0;

        Vector<CvSeq> squares = new Vector<CvSeq>();


        IplImage pyr = null, timg = null;
        timg = cvCloneImage(src);

        CvSize sz = cvSize(src.width() & -2, src.height() & -2);

        IplImage tgray = cvCreateImage(sz, IPL_DEPTH_8U, 1);
        IplImage gray = cvCreateImage(sz, IPL_DEPTH_8U, 1);

        pyr = cvCreateImage(cvSize(sz.width() / 2, sz.height() / 2), src.depth(), src.nChannels());

        // down-scale and upscale the image to filter out the noise
        cvPyrDown(timg, pyr, CV_GAUSSIAN_5x5);
        cvPyrUp(pyr, timg, CV_GAUSSIAN_5x5);

        CvSeq contours = new CvSeq(null);
        CvSeq approx;

        // request closing of the application when the image window is closed
        // show image on window
        // find squares in every color plane of the image


        tgray = cvCloneImage(timg);
        gray = cvCloneImage(timg);


        // try several threshold levels
        for (int l = 0; l < N; l++) {
            //             hack: use Canny instead of zero threshold level.
            //             Canny helps to catch squares with gradient shading

            if (l == 0) {
                //                apply Canny. Take the upper threshold from slider
                //                and set the lower to 0 (which forces edges merging)
                cvCanny(tgray, gray, 0, thresh, 3);
                //                 dilate canny output to remove potential
                //                // holes between edge segments
                cvDilate(gray, gray, null, 1);


            } else {

                val_thresh = (35 + l * 5);
                //                apply threshold if l!=0:
                cvThreshold(tgray, gray, val_thresh, 255, CV_THRESH_BINARY);
                cvSaveImage("tmp/g-" + l + ".jpg", gray);
            }


            //            find contours and store them all as a list
            cvFindContours(gray, storage, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);

            while (contours != null && !contours.isNull()) {

                if (contours.elem_size() > 0) {
                    approx = cvApproxPoly(contours, Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP, cvContourPerimeter(contours) * 0.02, 0);


                    if (approx.total() == 4
                            &&
                            Math.abs(cvContourArea(approx, CV_WHOLE_SEQ, 0)) > 50 &&
                            cvCheckContourConvexity(approx) != 0
                    ) {


                        double maxCosine = 0;
                        //
                        for (int j = 2; j < 5; j++) {
                            double cosine = Math.abs(angle(new CvPoint(cvGetSeqElem(approx, j)), new CvPoint(cvGetSeqElem(approx, j - 2)), new CvPoint(cvGetSeqElem(approx, j - 1))));
                            maxCosine = Math.max(maxCosine, cosine);
                        }
                        if (maxCosine < 0.4) {
                            CvRect x = cvBoundingRect(approx, 1);

                            if ((x.width() * x.height()) < 500000 && x.width() > x.height() && Math.abs(((float) x.height() / x.width()) - 0.2) < 0.1) {
                                squares.add(approx);

                            }
                        }
                    }
                }
                contours = contours.h_next();
            }
            contours = new CvSeq(null);

        }
        cvReleaseImage(timg);
        cvReleaseImage(tgray);
        cvReleaseImage(gray);
        cvReleaseImage(pyr);


        return squares;
    }


    public ResponseEntity<CarNumberResponse> recognize(String filePath) throws IOException {

        if (StringUtils.isEmpty(filePath)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } else {
            String[] words = filePath.trim().split(" ");
            FounderMgr mgr = new FounderMgr();

            if (words.length == 2 && words[1].equals("-v")) {
                mgr.verbose = true;
            }

            mgr.prepareEnv();

            // Если url
            if (filePath.toLowerCase().contains("http") && (filePath.toLowerCase().contains("jpg") || filePath.toLowerCase().contains("jpeg"))) {
                FounderMgr.println("Try to download file " + filePath);
                URL website = new URL(filePath);
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                filePath = FounderMgr.getPersonalTmpPath() + "/downloadedvc.jpg";
                FileOutputStream fos = new FileOutputStream(filePath);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                fos.close();
                FounderMgr.println("Download and save to " + filePath);
            }

            // Если файл на диске
            if (!Files.exists(Paths.get(filePath))) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // Распознаем
            Recognizer.process(filePath, mgr);

            mgr.println("Processing finished, " + mgr.getWorkTime() + " sec. remained");
            System.out.println(mgr.getResponse());
            ResponseEntity<CarNumberResponse> response = new ResponseEntity<CarNumberResponse>(mgr.getResponse(), HttpStatus.OK);
            mgr.destroy();
            return response;
        }
    }
}


