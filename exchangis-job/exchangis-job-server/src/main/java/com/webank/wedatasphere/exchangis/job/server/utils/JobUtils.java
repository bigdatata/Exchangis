package com.webank.wedatasphere.exchangis.job.server.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;


public class JobUtils {
    private static Logger logger = LoggerFactory.getLogger(JobUtils.class);

    /**
     * Replace the parameter in variables
     * @param map
     * @param key
     * @param subValue
     */
    private static void replaceParameters(Map<String, String> map, String key,
                                          StringBuffer subValue) {
        StringBuilder subKey = new StringBuilder();
        char[] chars = key.toCharArray();
        int i=0;
        boolean isParamter = false;
        int count = 0;
        while(i<chars.length){
            if(chars[i] == '$' && chars[i + 1] == '{'){
                count = 0;
                isParamter = true;
                if(count > 0){
                    subKey.append(chars[i]).append(chars[i+1]);
                }
                count++;
                i=i+2;
                continue;
            }else if(chars[i] == '}'){
                count--;
                if(count == 0){
                    String parameter = subKey.toString();
                    if(parameter.contains("${") && parameter.contains("}")){
                        StringBuffer sb = new StringBuffer();
                        replaceParameters(map, parameter, sb);
                        parameter = sb.toString();
                    }
                    String v = map.get(parameter);
                    isParamter = false;
                    subKey.delete(0, subKey.length());
                    if(null != v && !"".equals(v)){
                        if(v.contains("${") && v.contains("}")){
                            replaceParameters(map, v, subValue);
                        }else{
                            subValue.append(v);
                        }
                    }else{
                        subValue.append("${").append(parameter).append("}");
                    }
                    i=i+1;
                    continue;
                }

            }
            if(isParamter){
                subKey.append(chars[i]);
            }else{
                subValue.append(chars[i]);
            }
            i=i+1;
        }
    }


    public static String renderDt(String template, Calendar calendar){
        long time = calendar.getTimeInMillis();
        if(template==null){
            return null;
        }
        Date date =new Date();
        Matcher matcher= DateTool.TIME_REGULAR_PATTERN.matcher(template);
        while(matcher.find()){
            try {
                String m = template.substring(matcher.start(), matcher.end());
                StringWriter sw = new StringWriter();
                DateTool dataTool = new DateTool(time);
                String symbol = matcher.group(1);
                String lineSymbol = matcher.group(2);
                boolean spec = false;
                if (null != symbol) {
                    String startTime = null;
                    String tempTime = null;
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    if ("run_date".equals(symbol)) {
                        int n = 1;
                        if (m.split("-").length > 1) {
                            List<String> days = Arrays.asList(m.split("-"));
                            n = Integer.parseInt(days.get(1).substring(0, days.get(1).length()-1));
                        }
                        calendar.setTime(date);
                        calendar.add(Calendar.DAY_OF_MONTH, -n);
                        tempTime = format.format(calendar.getTime());
                        if(!DateTool.LINE_SYMBOL.equals(lineSymbol)){
                            tempTime=tempTime.replaceAll("-","");
                        }
                        startTime = template.replace(m, tempTime);
                        return startTime;
                    }

                    for(String specSymbol : DateTool.HOUR_SPEC_SYMBOLS){
                        if(specSymbol.equals(symbol)){
                            tempTime = dataTool.format(specSymbol);
                            startTime = template.replace(m, tempTime);
                            return startTime;
                        }
                    }
                    if (DateTool.MONTH_BEGIN_SYMBOL.equals(symbol)) {
                            dataTool.getMonthBegin(0);
                            tempTime = dataTool.format("yyyy-MM-dd HH:mm:ss");
                            startTime = template.replace(m, tempTime);
                            return startTime;
                        } else if (DateTool.MONTH_BEGIN_LAST_SYMBOL.equals(symbol)) {
                            dataTool.getMonthBeginLastDay(0);
                            tempTime = dataTool.format("yyyy-MM-dd HH:mm:ss");
                            startTime = template.replace(m, tempTime);
                            return startTime;
                        } else if (DateTool.TIME_PLACEHOLDER_SYMBOL.equals(symbol)){
                        calendar.setTime(date);
                        calendar.add(Calendar.DAY_OF_MONTH, -1);
                        tempTime = String.valueOf(calendar.getTimeInMillis());
                        startTime = template.replace(m, tempTime);
                        return startTime;
                        }
                }

            }catch(Exception e){
                logger.error("TASK_ERROR, cannot render job's configuration, message: {}", e.getMessage(), e);
                break;
            }
        }
        //${yesterday}
        return template.replace("${yesterday}",new DateTool(time).addDay(-1).format("yyyyMMdd"));
    }

}
