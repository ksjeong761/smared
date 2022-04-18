package kr.ac.kpu.block.smared;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptStringParser {
    // 영수증 OCR 결과 문자열로부터 날짜 정보를 추출한다.
    public String extractDate(String ocrResultString) {
        StringBuilder date = new StringBuilder();

        //ocrResultString = ocrResultString.replaceAll("[^0-9\\.\\,\\-\\n\\/년월일]","");
        Pattern pattern = Pattern.compile("(19|20)\\d{2}[-/.년]*([1-9]|0[1-9]|1[012])[-/.월]*(0[1-9]|[12][0-9]|3[01])", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ocrResultString);
        while (matcher.find()) {
            date.append(matcher.group());
        }

        return date.toString().replaceAll("[년월일/.]","-");
    }

    // 구매한 물건의 이름, 수량, 가격 정보가 있는 행을 추출한다.
    public List<String> extractProductInfoRow(String ocrResultString) {
        List<String> productInfoRows = new ArrayList<>();

        // 줄 바꿈 문자를 기준으로 문자열을 분리한다.
        boolean skipUselessData = true;
        StringTokenizer linefeedTokenizer = new StringTokenizer(ocrResultString,"\n");
        while (linefeedTokenizer.hasMoreTokens()){
            String productInfoRow = linefeedTokenizer.nextToken();

            // 상품 정보 이전의 데이터는 무시한다.
            if (skipUselessData) {
                if (productInfoRow.contains("금액")) {
                    skipUselessData = false;
                }
                continue;
            }

            // 상품 정보 이후의 데이터는 무시한다.
            if (productInfoRow.contains("공급") || productInfoRow.contains("면세") || productInfoRow.contains("과세") || productInfoRow.contains("주문") || productInfoRow.contains("금액")) {
                break;
            }

            // 중복되지 않은 데이터만 추출한다.
            if (!productInfoRows.contains(productInfoRow)) {
                productInfoRows.add(productInfoRow);
            }
        }

        return productInfoRows;
    }

    // 영수증 OCR 결과 문자열로부터 가격 정보를 추출한다.
    public List<String> extractPrice(String ocrResultString) {
        List<String> prices = new ArrayList<>();

        //ocrResultString = ocrResultString.replaceAll("[^0-9\\.\\,\\n\\s]","");
        // 줄 바꿈 문자를 기준으로 문자열을 분리한다.
        boolean skipUselessData = true;
        StringTokenizer linefeedTokenizer = new StringTokenizer(ocrResultString,"\n");
        while (linefeedTokenizer.hasMoreTokens()) {
            String productInfoRow = linefeedTokenizer.nextToken();

            // 상품 정보 이전의 데이터는 무시한다.
            if (skipUselessData) {
                if (productInfoRow.contains("금액")) {
                    skipUselessData = false;
                }
                continue;
            }

            // 상품 정보 이후의 데이터는 무시한다.
            if (productInfoRow.contains("공급") || productInfoRow.contains("면세") || productInfoRow.contains("과세") || productInfoRow.contains("주문") || productInfoRow.contains("금액")) {
                break;
            }

            // 가격을 얻어야 하므로 숫자 이외의 데이터, 쉼표가 없는 데이터는 무시한다.
            if (!productInfoRow.contains("0")) continue;
            if (!productInfoRow.contains(",") && !productInfoRow.contains(".")) continue;

            // 스페이스바를 기준으로 문자열을 분리
            StringTokenizer spaceTokenizer = new StringTokenizer(productInfoRow, " ");
            while (spaceTokenizer.hasMoreTokens()){
                String word = spaceTokenizer.nextToken();

                // 가격을 얻어야 하므로 숫자 이외의 데이터, 쉼표가 없는 데이터는 무시한다.
                if (word.charAt(word.length() - 1) != '0') continue;
                if (!word.contains(",") && !word.contains(".")) continue;

                // 쉼표를 제거하고 숫자 값만 얻는다.
                word = word.replace(",", "");
                word = word.replace(".", "");

                // 정보를 수집한다.
                if (!prices.contains(word)) {
                    prices.add(word);
                }
            }
        }

        return prices;
    }

    // 영수증 OCR 결과 문자열로부터 한글 텍스트 정보를 추출한다.
    public List<String> extractKoreanText(String ocrResultString) {
        List<String> koreanTexts = new ArrayList<>();

        // 줄 바꿈 문자를 기준으로 문자열을 분리한다.
        boolean skipUselessData = true;
        StringTokenizer linefeedTokenizer = new StringTokenizer(ocrResultString,"\n");
        while (linefeedTokenizer.hasMoreTokens()) {
            String productInfoRow = linefeedTokenizer.nextToken();

            // 상품 정보 이전의 데이터는 무시한다.
            if (skipUselessData) {
                if (productInfoRow.contains("금액")) {
                    skipUselessData = false;
                }
                continue;
            }

            // 상품 정보 이후의 데이터는 무시한다.
            if (productInfoRow.contains("공급") || productInfoRow.contains("면세") || productInfoRow.contains("과세") || productInfoRow.contains("주문") || productInfoRow.contains("금액")) {
                break;
            }

            // 한글을 포함하는 데이터를 발견하면 한글 이외의 데이터를 지운다.
            if (productInfoRow.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*")) {
                String koreanText = productInfoRow.replaceAll("[^[ㄱ-ㅎㅏ-ㅣ가-힣]\\n]", "");

                // 정보를 수집한다.
                if (!koreanTexts.contains(koreanText)) {
                    koreanTexts.add(koreanText);
                }
            }
        }

        return koreanTexts;
    }
}
