package com.andrew.hnt.api.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class StringUtil {

    /**
     * 
     * @param arg
     * @return
     */
    public static boolean isEmpty(Object arg) {
        return null == arg || "".equals(arg) || "null".equalsIgnoreCase(String.valueOf(arg));
    }

    /**
     * 
     * @param str
     * @return
     */
    public static boolean isEmpty(String str) {
        return (str == null || "".equals(str) || str.trim().length() < 1 || "null".equalsIgnoreCase(str));
    }

    /**
     * 
     * @param str
     * @return
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 
     * @param arg
     * @return
     */
    public static boolean isNotEmpty(Object arg) {
        return !isEmpty(arg);
    }
    
    /**
     * 
     * @param str
     * @param def
     * @return
     */
    public static String defaultString(String str, String def) {
        if (isEmpty(str)) {
            return def;
        } else {
            return str;
        }
    }
    
    /**
     * 
     * @param arg
     * @param def
     * @return
     */
    public static String defaultString(Object arg, String def) {
        if (arg instanceof String) {
            return defaultString(String.valueOf(arg), def);
        } else {
            return isEmpty(arg) ? def : String.valueOf(arg);
        }
    }
    
    /**
     * 
     * @param str
     * @return
     */
    public static String defaultString(String str) {
        return defaultString(str, "");
    }
    
    /**
     * 
     * @param arg
     * @return
     */
    public static String defaultString(Object arg) {
        return defaultString(arg, "");
    }
    
    /**
     * 
     * @param target
     * @param trimType
     * @param trimChar
     * @return
     */
    public static String trim(String target, String trimType, char trimChar) {
                
        String result = "";
                
        if ( "L".equals(trimType) ) {
            int targetIndex = 0;        
            for ( int i = targetIndex ; i < target.length() ; i++ ) {
                if (trimChar == target.charAt(i)) {
                    targetIndex++;
                } else {
                    result = target.substring(targetIndex, target.length());
                    break;
                }
            }
        } else {
            int targetIndex = target.length()-1;        
            for ( int i = targetIndex ; i >= 0 ; i-- ) {
                if ( trimChar == target.charAt(i) ) {
                    targetIndex--;
                } else {
                    result = target.substring(0, targetIndex+1);
                    break;
                }
            }
        }
        
        return result;
    }
    
    /**
     * 
     * @param target
     * @param trimChar
     * @return
     */
    public static String trimLeft(String target, char trimChar) {
        return trim(target, "L", trimChar);
    }
    
    /**
     * 
     * @param target
     * @param trimChar
     * @return
     */
    public static String trimRight(String target, char trimChar) {
        return trim(target, "R", trimChar);
    }
    
    /**
     * 
     * @param target
     * @return
     */
    public static String trimNumber(String target) {
        return trim(target, "L", '0');
    }
    
    /**
     * 
     * @param target
     * @return
     */
    public static String trimString(String target) {
        return trim(target, "R", ' ');
    }
    
    /**
     * 
     * @param map
     * @return
     */
    public static Map<String, Object> trimMap(Map<String, Object> map) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        
        Iterator<String> keys = map.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (map.get(key) instanceof String && !isEmpty(map.get(key))) {
                String value = (String) map.get(key);
                resultMap.put(key, value.trim());
            } else {
                resultMap.put(key, map.get(key));               
            }
        }
        
        return resultMap;
    }
    
    /**
     * 
     * @param src
     * @return
     */
    public static String escapeString(String src) {
        if (isEmpty(src)) {
            return "";
        }
        
        return src.replaceAll("#", "&#35;")
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("\'", "&apos;");
    }
    
    /**
     * BO에서 escape된 String을 복원
     * @param src
     * @return
     */
    public static String unEscapeString(String src) {
        if (isEmpty(src)) {
            return "";
        }
        
        return src.replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&#40;", "\\(")
                .replaceAll("&#41;", "\\)")
                .replaceAll("&#39;", "'");
    }
    
    /**
     * 
     * @param src
     * @return
     */
    public static String escapeString2(String src) {
        if (isEmpty(src)) {
            return "";
        }
        
        return src.replaceAll(";", "&#59;")
                .replaceAll("#", "&#35;")
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\\(", "&#40;")
                .replaceAll("\\)", "&#41;")
                .replaceAll("\\{", "&#123;")
                .replaceAll("\\}", "&#125;")
                .replaceAll("\"", "&quot;")
                .replaceAll("\'", "&apos;");
    }
    
    /**
     * 
     * @param src
     * @return
     */
    public static String nl2br(String src) {
        if (isEmpty(src)) {
            return "";
        }
        
        return src.replaceAll("\r", "<br />")
                .replaceAll("\n", "<br />")
                .replaceAll("\r\n", "<br />");
    }
    
    /**
     * 
     * @param e
     * @return
     */
    public static String printStackTraceToString(Exception e) {
        StringBuffer sb = new StringBuffer();

        try {
            sb.append(e.toString());
            sb.append("\n");
            StackTraceElement element[] = e.getStackTrace();
            for (int idx = 0; idx < element.length; idx++) {
                sb.append("\tat ");
                sb.append(element[idx].toString());
                sb.append("\n");
            }
        } catch (Exception ex) {
            return e.toString();
        }
        return sb.toString();
    }
    
    /**
     * 
     * @param str
     * @param splitIndex
     * @return
     */
    public static List<String> stringToList(String str, int splitIndex) {
        List<String> result = new ArrayList<String>();
        int strLength = str.length();
        
        if(strLength < splitIndex){
            result.add(str);
        }else{
            int beginIndex = 0;
            int endIndex = 0;
            while (endIndex < strLength) {
                beginIndex = endIndex;
                endIndex = endIndex + splitIndex;
                if(endIndex > strLength){
                    endIndex = strLength;
                }
                result.add(str.substring(beginIndex, endIndex));
            }
        }
        
        return result;
    }
        
    /**
     * 
     * @param szText
     * @param szKey
     * @param nLength
     * @param nPrev
     * @param isNotag
     * @param isAdddot
     * @return
     */
    public static String stringByteCut(String szText, String szKey, int nLength, int nPrev, boolean isNotag, boolean isAdddot){  // 문자열 자르기

        String rVal = szText;
        int oF = 0, oL = 0, rF = 0, rL = 0;
        int nLengthPrev = 0;
        Pattern p = Pattern.compile("<(/?)([^<>]*)?>", Pattern.CASE_INSENSITIVE);  // 태그제거 패턴

        if(isNotag) {rVal = p.matcher(rVal).replaceAll("");}  // 태그 제거
        rVal = rVal.replaceAll("&", "&");
        rVal = rVal.replaceAll("(!/|\r|\n| )", "");  // 공백제거

        try {
            byte[] bytes = rVal.getBytes("UTF-8");     // 바이트로 보관

            if(szKey != null && !szKey.equals("")) {
                nLengthPrev = (rVal.indexOf(szKey) == -1)? 0: rVal.indexOf(szKey);  // 일단 위치찾고
                nLengthPrev = rVal.substring(0, nLengthPrev).getBytes("MS949").length;  // 위치까지길이를 byte로 다시 구한다
                nLengthPrev = (nLengthPrev-nPrev >= 0)? nLengthPrev-nPrev:0;    // 좀 앞부분부터 가져오도록한다.
            }

            // x부터 y길이만큼 잘라낸다. 한글안깨지게.
            int j = 0;

            if(nLengthPrev > 0) while(j < bytes.length) {
                if((bytes[j] & 0x80) != 0) {
                    oF+=2; rF+=3; if(oF+2 > nLengthPrev) {break;} j+=3;
                } else {if(oF+1 > nLengthPrev) {break;} ++oF; ++rF; ++j;}
            }

            j = rF;

            while(j < bytes.length) {
                if((bytes[j] & 0x80) != 0) {
                    if(oL+2 > nLength) {break;} oL+=2; rL+=3; j+=3;
                } else {if(oL+1 > nLength) {break;} ++oL; ++rL; ++j;}
            }

            rVal = new String(bytes, rF, rL, "UTF-8");  // charset 옵션

            /*if (isAdddot && ((rF + rL + 3) <= bytes.length)) {
                rVal.concat("...");
            } */ // ...을 붙일지말지 옵션
        } catch(UnsupportedEncodingException e){ e.printStackTrace(); }  

        return rVal;
    }
    
    /**
     * 
     * @param text
     * @return
     */
    public static String escapeXml(String text) {
        if (null == text || text.isEmpty()) {
            return text;
        }
        final int len = text.length();
        char current = 0;
        int codePoint = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            current = text.charAt(i);
            boolean surrogate = false;
            if (Character.isHighSurrogate(current)
                    && i + 1 < len && Character.isLowSurrogate(text.charAt(i + 1))) {
                surrogate = true;
                codePoint = text.codePointAt(i++);
            } else {
                codePoint = current;
            }
            if ((codePoint == 0x9) || (codePoint == 0xA) || (codePoint == 0xD)
                    || ((codePoint >= 0x20) && (codePoint <= 0xD7FF))
                    || ((codePoint >= 0xE000) && (codePoint <= 0xFFFD))
                    || ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF))) {
                sb.append(current);
                if (surrogate) {
                    sb.append(text.charAt(i));
                }
            }
        }
        
        return sb.toString();
    }
    

    public static boolean isEmptyOrWhitespace(String text) {
        String text_data = makeSafe(text);
        
        for (int i = 0, n = text_data.length(); i < n; i++) {
            if (!Character.isWhitespace(text_data.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String makeSafe(String s) {
        return (s == null) ? "" : s;
    }
    
    /*
    public static String replace(String str, String replacedStr,
            String replaceStr) {
        String newStr = "";
        if (str.indexOf(replacedStr) != -1) {
            String s1 = str.substring(0, str.indexOf(replacedStr));
            String s2 = str.substring(str.indexOf(replacedStr) + 1);
            newStr = s1 + replaceStr + s2;
        }
        return newStr;
    }
    */
    
    public static boolean equals(String source, String target) {

        return null2void(source).equals(null2void(target));

    }
    
    public static String null2void(String data) {
        
        String returnData = "";
        
        if (isNull(data)) {
            returnData = "";
        }else {
            returnData = data;
        }

        return returnData;
    }
    
    public static boolean isNull(String data) {
        
        String returnData = "";
        
        if (data != null) {
            returnData = data.trim();
        }

        return (returnData == null || "".equals(returnData));
    }

    public static String subStrByte(String target, int byteSize) {
        byte[] bytes = target.getBytes();
        String returnValue = target;

        if(bytes.length > byteSize ) {
            returnValue = new String(bytes, 0, byteSize );
        }
        return returnValue;
    }
    
    public static String getLocalDateTime() {
		Calendar calLocal = Calendar.getInstance();
		return "" + calLocal.get(Calendar.YEAR)
				+ makeTowDigit(calLocal.get(Calendar.MONTH) + 1)
				+ makeTowDigit(calLocal.get(Calendar.DATE))
				+ makeTowDigit(calLocal.get(Calendar.HOUR_OF_DAY))
				+ makeTowDigit(calLocal.get(Calendar.MINUTE))
				+ makeTowDigit(calLocal.get(Calendar.SECOND));
	}
  
    protected static String makeTowDigit(int num) {
		return (num < 10 ? "0" : "") + num;
	}
  
    public static String getLocalDateTime(String format) {
		SimpleDateFormat fmt = new SimpleDateFormat(format);
		long time = System.currentTimeMillis();
		String strTime = fmt.format(new Date(time));
		return strTime;
	}
  
    public static Date toDate(String date, String format) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.parse(date);
	}
    
    public static String getMaskingIp() {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();


        String ipAddr = request.getHeader("X-Forwarded-For");
        if (ipAddr == null || ipAddr.length() == 0 || "unknown".equalsIgnoreCase(ipAddr)) {
            ipAddr = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddr == null || ipAddr.length() == 0 || "unknown".equalsIgnoreCase(ipAddr)) {
            ipAddr = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddr == null || ipAddr.length() == 0 || "unknown".equalsIgnoreCase(ipAddr)) {
            ipAddr = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddr == null || ipAddr.length() == 0 || "unknown".equalsIgnoreCase(ipAddr)) {
            ipAddr = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddr == null || ipAddr.length() == 0 || "unknown".equalsIgnoreCase(ipAddr)) {
            ipAddr = request.getRemoteAddr();
        }

    	StringBuffer maskingIpAddr = new StringBuffer();
    	
    	if(ipAddr != null) {
    		String[] arrIpAddr = ipAddr.split("\\.");
        	if(arrIpAddr != null && arrIpAddr.length == 4) {
        		maskingIpAddr = maskingIpAddr.append("***.").append(arrIpAddr[1]).append(".***.").append(arrIpAddr[3]);
        	}else {
        		maskingIpAddr = maskingIpAddr.append(ipAddr);
        	}
    	}
    	return maskingIpAddr.toString();
    }
    
    public static String getMaskingCardNum(String cardNum) {
    	StringBuffer maskingCardNum = new StringBuffer();
    	
    	if(cardNum != null) {
    		if(cardNum.length() >= 16) {
    			maskingCardNum = maskingCardNum.append(cardNum.substring(0, 4)).append("-").append(cardNum.substring(4, 8)).append("-****-").append(cardNum.substring(12, 16));
    		} else {
    			if(cardNum.length() >= 4 && cardNum.length() < 16) {    // 카드번호 자리수가 4자리 초과, 16자리 미만인 경
	    			// 카드번호 자리수가 16자리 미만인 경우 4 ~ 8번째 자리수를 * 로 치환 처리 추가 2020-02-17 by Andrew Kim
	    			cardNum = cardNum.replace(cardNum.substring(4, 8), "****");
	    			maskingCardNum.append(cardNum);
	    		} else if(cardNum.length() < 4) {    // 카드번호 자리수가 4자리 이하인 경우 전체 자리수를 모두 * 로 치환 처리 추가 2020-02-17 by Andrew Kim
	    			cardNum = cardNum.replace(cardNum.substring(0, cardNum.length()), "****");
	    			maskingCardNum.append(cardNum);
	    		}
    		}
    	}
    	
    	return maskingCardNum.toString();
    }
    
    public static String getMaskingAccNum(String accNum) {
    	StringBuffer maskingAccNum = new StringBuffer();
    	
    	if(accNum != null) {
    		int accNumLength = accNum.length();
    		
    		if(accNumLength > 8) {
    			maskingAccNum = maskingAccNum.append(accNum.substring(0, accNumLength-8)).append("****").append(accNum.substring(accNumLength-4, accNumLength));
    		}else if(8 >= accNumLength && accNumLength > 4){
    			maskingAccNum = maskingAccNum.append("****").append(accNum.substring(accNumLength-4, accNumLength));
    		}else {
    			maskingAccNum = maskingAccNum.append(accNum);
    		}
    	}
    	
    	return maskingAccNum.toString();
    }
    
    public static String getMaskingCPNum(String cpNum) {
    	StringBuffer maskingCpNum = new StringBuffer();
    	
    	if(cpNum != null) {
    		String tmpCpNum = cpNum.replaceAll("-", "");
    		int cpNumLength = tmpCpNum.length();
    		
    		if(cpNumLength == 11) {
    			maskingCpNum = maskingCpNum.append(tmpCpNum.substring(0, 3)).append("-").append(tmpCpNum.substring(3, 5)).append("**-*").append(tmpCpNum.substring(8, 11));
    		}else if(cpNumLength == 10){
    			maskingCpNum = maskingCpNum.append(tmpCpNum.substring(0, 3)).append("-").append(tmpCpNum.substring(3, 4)).append("**-*").append(tmpCpNum.substring(7, 10));
    		}else {
    			maskingCpNum = maskingCpNum.append(cpNum);
    		}
    	}
    	
    	return maskingCpNum.toString();
    }
    
    public static String getMaskingEmail(String email) {
    	StringBuffer maskingEmail = new StringBuffer();
    	
    	if(email != null) {
    		String[] splitEmail = email.split("@", 2);
    		if(splitEmail.length >=2) {
    			if(splitEmail[0].length()<=3) {
    				maskingEmail = maskingEmail.append("***@").append(splitEmail[1]);
    			}else {
    				maskingEmail = maskingEmail.append(splitEmail[0].substring(0, splitEmail[0].length()-3)) .append("***@").append(splitEmail[1]);
    			}
    		}else {
    			maskingEmail = maskingEmail.append("***").append(splitEmail[0].substring(0, splitEmail[0].length()));
    		}
    	}
    	
    	return maskingEmail.toString();
    }
    
    public static String setComma(String amt) {
    	try {
			return NumberFormat.getInstance().format(Integer.parseInt(amt));
		}catch(Exception e) {
			return "0";
		}
    }
}