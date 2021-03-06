package com.ilgusi.service.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.ilgusi.category.model.vo.Category;
import com.ilgusi.favorite.model.vo.Favorite;
import com.ilgusi.member.model.service.MemberService;
import com.ilgusi.member.model.vo.Member;
import com.ilgusi.question.model.vo.Question;
import com.ilgusi.service.model.service.ServiceService;
import com.ilgusi.service.model.vo.Join;
import com.ilgusi.service.model.vo.ReviewPageData;
import com.ilgusi.service.model.vo.Service;
import com.ilgusi.service.model.vo.ServiceFile;
import com.ilgusi.service.model.vo.ServicePageData;
import com.ilgusi.service.model.vo.ServicePay;
import com.ilgusi.service.model.vo.ServiceReview;

import common.FileNameOverlap;

@Controller
public class ServiceController {
	@Autowired
	private ServiceService service;

	@Autowired
	private MemberService memberService;

	@RequestMapping("/introduceFrm.do")
	public String introduceFrm(String mId, int reqPage, Model model) {
		Join j = service.selectOneMember(mId);
		// 승인된 서비스만 가져오기
		// 전체 서비스리스트
		List<Service> serviceList = service.serviceList(mId);
		// 승인된 서비스 리스트
		List<Service> approvedList = new ArrayList<Service>();
		for (int i = 0; i < serviceList.size(); i++) {
			char approval = serviceList.get(i).getAdminApproval();
			char deleted = serviceList.get(i).getDeleteStatus();

			if (approval == 'y' && deleted == 'n') {
				approvedList.add(serviceList.get(i));
			}
		}
		j.setServiceList(approvedList);
		// 후기리스트

		Join join = new Join();
		if (service.selectReviewList(mId, reqPage) != null) {
			join = service.selectReviewList(mId, reqPage);
			j.setReviewList(join.getReviewList());
		}

		float avg = service.sRateAVG(mId);
		model.addAttribute("avg", avg);

		// System.out.println(list);
		// System.out.println("리뷰리스트" + join.getReviewList());
		model.addAttribute("pageNavi", join.getPageNavi());
		model.addAttribute("j", j);
		return "freelancer/introduce";
	}

	// (영재) 리뷰갯수 구하기
	@RequestMapping("/reviewListSize.do")
	public void reviewListSize(String mId, Model model) {
		List<ServiceReview> list = service.reviewListSize(mId);
		System.out.println("mid>>>>>" + mId);
		model.addAttribute("list", list);
		System.out.println("list>>>>>>평점" + list);
	}
	// (영재) 평점 평균 구하기
	/*
	 * @RequestMapping("/sRateAVG.do") public void sRateAVG(String mId,Model model)
	 * { System.out.println("midRate>>>>>>>>전>"+mId); List<Service> list =
	 * service.sRateAVG(mId); System.out.println("midRate>>>>>>>>>"+mId);
	 * model.addAttribute("list",list); System.out.println("list>>>>>>평균점수"+list); }
	 */
	// (영재)

	@RequestMapping("/serviceJoinFrm.do")
	public String serviceJoinFrm() {
		return "freelancer/servicejoin";
	}

	@RequestMapping("/serviceJoin.do")
	public String serviceJoin(Join join, Model model, MultipartFile[] ssImg, HttpServletRequest request) {
		String root = request.getSession().getServletContext().getRealPath("/");
		String path = root + "/upload/service/";
		System.out.println("경로는 : " + path);
		ArrayList<ServiceFile> fileList = new ArrayList<ServiceFile>();

		for (MultipartFile file : ssImg) { // 파일이 여러개라 반복문으로 처리
			String filename = file.getOriginalFilename();
			System.out.println("파일 이름" + filename);
			String filepath = new FileNameOverlap().rename(path, filename);
			System.out.println("filename : " + filename);
			System.out.println("filepath : " + filepath);

			try {
				byte[] bytes = file.getBytes();
				File upFile = new File(path + filepath);
				FileOutputStream fos = new FileOutputStream(upFile);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				bos.write(bytes);
				bos.close();

				ServiceFile f = new ServiceFile();
				f.setFilename(filename);
				f.setFilepath(filepath);
				fileList.add(f); // 데이터베이스 처리를 위해 객체화 해서 list에 추가
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		join.setFileList(fileList);
		join.setSImg(fileList.get(0).getFilepath());
		int result = service.insertService(join);
		if (result > 0) {
			model.addAttribute("msg", "서비스를 등록하였습니다");
		} else {
			model.addAttribute("msg", "서비스등록실패");
		}
		model.addAttribute("loc", "/freelancerServiceList.do?mId=" + join.getMId() + "&order=refuse");
		return "common/msg";
	}

	// 프리랜서 마이페이지 이동
	@RequestMapping("/freelancerMypage.do")
	public String selectfreelancerMypage(int MNo, Model model) {
		Member m = service.selectOneMember(MNo);
		int serviceCount = service.selectFreeServiceCount(m.getMId());
		model.addAttribute("m", m);
		model.addAttribute("serviceCount", serviceCount);
		return "freelancer/freelancerMypage";
	}

	// 프리랜서 마이페이지 -> 서비스 리스트 이동
	@RequestMapping("/freelancerServiceList.do")
	public String freelancerServiceList(String mId, Model model, String order) {
		Join j = new Join();
		j.setServiceList(service.serviceList(mId));
		ArrayList<Service> list = service.selectMyList(mId, order);

		DecimalFormat formatter = new DecimalFormat("###,###");
		for (int i = 0; i < list.size(); i++) {
			list.get(i).setSPriceTxt(formatter.format(list.get(i).getSPrice()));
		}

		model.addAttribute("list", list);
		System.out.println("list사이즈 : " + list.size());
		model.addAttribute("j", j);
		System.out.println("test" + j.getServiceList().size());
		if (list.size() != 0) {
			System.out.println("메인카테고리이름:" + list.get(0).getMainCategoryName());
		}

		model.addAttribute("order", order);
		return "freelancer/freelancerServiceList";
	}

	// 프리랜서 마이페이지 정보수정(소개글,연락가능시간,브랜드명 추가)
	@RequestMapping("/updateFreelancer.do")
	public String updateFreelancer(Member m, Model model, HttpServletRequest req) {
		int result = service.updateFreelancer(m);
		if (result > 0) {
			m = memberService.loginMember(m.getMId(), m.getMPw());
			model.addAttribute("msg", "등록되었습니다.");
			if (m != null) {
				HttpSession session = req.getSession();
				session.setAttribute("loginMember", m);
			}
		}
		model.addAttribute("loc", "/freelancerMypage.do?MNo=" + m.getMNo());
		return "common/msg";
	}

	// 프리랜서 마이페이지 - 서비스 삭제하기
	@RequestMapping("/delService.do")
	public String deleteService(int sNo, Model model, String mId) {
		int result = service.deleteService(sNo);
		if (result != 0) {
			model.addAttribute("msg", "서비스가 삭제되었습니다.");
		}
		model.addAttribute("loc", "/freelancerServiceList.do?mId="+mId+"&order=agree");
		return "common/msg";
	}

	// (문정)사용자 마이페이지 - 거래후기 쓰기
	@RequestMapping("/serviceReviewWrite.do")
	public String serviceReviewWrite(int tNo, int sNo, String mId, String sImg, String sTitle, Model model) {
		model.addAttribute("tNo", tNo);
		model.addAttribute("sNo", sNo);
		model.addAttribute("mId", mId);
		model.addAttribute("sImg", sImg);
		model.addAttribute("sTitle", sTitle);
		return "service/serviceReviewWrite";
	}

	// (문정) 마이페이지 - 서비스 후기 등록
	@RequestMapping("/serviceReviewInsert.do")
	public String serviceReviewInsert(ServiceReview data, Model model) {
		int result = service.serviceReviewInsert(data);
		if (result > 0) {
			result = service.serviceReviewSuccess(data.getTNo());
			if (result > 0) {
				model.addAttribute("msg", "리뷰를 등록하였습니다.");
				result = service.serviceUpdateSRate(data.getSNo());
				if (result > 0)
					System.out.println("[등록]서비스테이블에 s_rate 수정 성공");
			}
		}
		return "/service/reviewDone";
	}

	// (문정) 마이페이지 - 거래 후기 작성한거 확인
	@RequestMapping("/serviceReviewView.do")
	public String serviceReviewView(ServiceReview data, Model model) {
		ServiceReview sr = service.serviceReviewView(data);
		model.addAttribute("review", sr);
		return "/service/serviceReviewUpdate";
	}

	// (문정) 서비스 리뷰 수정
	@RequestMapping("/serviceReviewUpdate.do")
	public String serviceReviewUpdate(ServiceReview review, Model model) {
		int result = service.serviceReviewUpdate(review);
		if (result > 0) {
			model.addAttribute("msg", "리뷰를 수정하였습니다.");
			result = service.serviceUpdateSRate(review.getSNo());
			if (result > 0)
				System.out.println("[수정]서비스테이블에 s_rate 수정 성공");
		}
		return "/service/reviewDone";
	}

	// (문정) 서비스 리뷰 삭제
	@RequestMapping("/serviewReviewDelete.do")
	public String serviewReviewDelete(int rNo, int tNo, int sNo, Model model) {
		int result = service.serviceReviewDelete(rNo);
		if (result > 0) {
			result = service.serviceTradeStatusUpdate(tNo);
			if (result > 0) {
				model.addAttribute("msg", "리뷰를 삭제했습니다.");
				result = service.serviceUpdateSRate(sNo);
				if (result > 0)
					System.out.println("[삭제]서비스테이블에 s_rate 수정 성공");
			}
		}
		return "/service/reviewDone";
	}

	// (다솜)serviceList
	@RequestMapping("/serviceList.do")
	public String serviceList(int cNo, int reqPage, String order, Model model, String keyword) {

		int numPerPage = 12;
		int end = reqPage * numPerPage;
		int start = end - numPerPage + 1;

		Service s = new Service();
		System.out.println("cNo : " + cNo);
		int maincateNum = 0;
		int subNo = 0;

		HashMap<String, Object> map = new HashMap<String, Object>();

		if (cNo % 10 == 0) {
			maincateNum = cNo;
			s.setSubCategory(subNo);
			map.put("sub", 0);

		} else {
			maincateNum = (cNo / 10) * 10;
			subNo = cNo;
			map.put("sub", subNo);
		}

		map.put("main", maincateNum);
		map.put("start", start);
		map.put("end", end);
		map.put("reqPage", reqPage);
		map.put("cNo", cNo);
		map.put("keyword", keyword);
		map.put("order", order);

		/* map.put("order", order); */

		s.setMainCategory(maincateNum);
		s.setSubCategory(subNo);
		System.out.println("메인카테고리 : " + maincateNum);
		System.out.println("서브카테고리 : " + subNo);

		// 카테고리 리스트 불러오기
		ArrayList<Category> catList = service.categoryList(maincateNum);
		System.out.println("카테고리 리스트 사이즈 : " + catList.size());
		System.out.println("catList(1)값 : " + catList.get(1));

		// 서비스 리스트 불러오기

		ServicePageData spd = new ServicePageData();
		spd.setEnd(end);
		spd.setKeyword(keyword);
		spd.setReqPage(reqPage);
		spd.setStart(start);
		spd.setCNo(cNo);

		// 서비스 리스트 불러오기+페이징
		spd = service.servicePageList(map, reqPage, cNo, order);
		ArrayList<Service> serList = spd.getList();

		// 가격 => 천단위 콤마 찍기
		DecimalFormat formatter = new DecimalFormat("###,###");
		for (int i = 0; i < serList.size(); i++) {
			serList.get(i).setSPriceTxt(formatter.format(serList.get(i).getSPrice()));
		}

		// 맵 확인용 ArrayList
		ArrayList<HashMap<String, Object>> mapList = new ArrayList<HashMap<String, Object>>();
		mapList.add(map);

		if (mapList.size() != 0) {
			System.out.println("mapList(0) : " + mapList.get(0));
		}

		if (cNo % 10 == 0) {
			for (int i = 0; i < serList.size(); i++) {
				serList.get(i).setSubCategory(0);
			}
		}

		if (serList.size() > 0) {
			System.out.println("serList 사이즈 : " + serList.size());
			System.out.println("serList.get(0) : " + serList.get(0));
			System.out.println("serList.get(0).평점 : " + serList.get(0).getSRate());
			model.addAttribute("serviceList", spd.getList());

		} else if (serList.size() == 0) {
			System.out.println("serList 사이즈 : " + serList.size());
			model.addAttribute("noServiceList", "noServiceList");
		}

		if (cNo % 10 == 0) {
			model.addAttribute("c_no", serList.get(0).getMainCategory());
		} else {
			model.addAttribute("c_no", serList.get(0).getSubCategory());
		}

		switch (maincateNum) {
		case 10:
			model.addAttribute("mainCate", "디자인");
			break;
		case 20:
			model.addAttribute("mainCate", "ITㆍ프로그래밍");
			break;
		case 30:
			model.addAttribute("mainCate", "영상ㆍ사진ㆍ음향");
			break;
		case 40:
			model.addAttribute("mainCate", "교육");
			break;
		case 50:
			model.addAttribute("mainCate", "문서ㆍ글쓰기");
			break;
		case 60:
			model.addAttribute("mainCate", "비즈니스컨설팅");
			break;
		case 70:
			model.addAttribute("mainCate", "주문제작");
			break;
		}

		model.addAttribute("catList", catList);
		model.addAttribute("pageNavi", spd.getPageNavi());
		model.addAttribute("order", order);

		return "/service/serviceList";
	}

	// (다솜) serviceView 페이지 이동
	@RequestMapping("/serviceView.do")
	public String serviceView(int sNo, Model model, int reqPage, int mNo) {
		System.out.println("서비스 컨트롤러-serviceView");
		System.out.println("서비스 상세보기 sNo: " + sNo);

		// 해당 서비스 정보 불러오기
		Service s = service.selectServiceView(sNo);

		DecimalFormat formatter = new DecimalFormat("###,###");
		s.setSPriceTxt(formatter.format(s.getSPrice()));

		System.out.println("메인카테고리 이름 : " + s.getMainCategoryName());
		System.out.println("서브카테고리 이름 : " + s.getSubCategoryName());

		if (s != null) {
			model.addAttribute("s", s);
		}
		int serviceNo = s.getSNo();

		// 브랜드 정보 불러오기
		String memberId = s.getMId();

		Member m = service.selectMemberName(memberId);
		model.addAttribute("m", m);

		// 서비스 파일 불러오기
		ArrayList<ServiceFile> fileList = service.fileList(sNo);
		System.out.println("fileList 사이즈 :" + fileList.size());
		System.out.println("fileList값:" + fileList.get(0));
		model.addAttribute("fileList", fileList);

		// 해당 유저가 등록한 다른서비스 불러오기
		ArrayList<Service> sList = service.userService(memberId);
		
		model.addAttribute("sList", sList);

		// 리뷰 리스트 불러오기 + 페이징
		ReviewPageData rpd = service.selectReviewList(sNo, reqPage,mNo);
		if (rpd.getList().size() == 0) {
			System.out.println("리뷰 없음");
		} else {
			System.out.println("리뷰있음");
			System.out.println(rpd.getPageNavi());
		}

		model.addAttribute("reviewList", rpd.getList());
		model.addAttribute("pageNavi", rpd.getPageNavi());

		// 찜한내역 확인하기

		Favorite f = service.searchFavorite(mNo, serviceNo);
		if (f != null) {
			System.out.println("찜하기 있음 ");
			model.addAttribute("favoriteCheck", true);
		} else {
			System.out.println("찜하기 없음");
			model.addAttribute("favoriteCheck", false);
		}
		return "/service/serviceView";
	}

	// (다솜) serviceView 페이지 이동 (로그인 안됐을때)
	@RequestMapping("/serviceView2.do")
	public String serviceView(int sNo, Model model, int reqPage) {
		System.out.println("서비스 컨트롤러-serviceView");
		System.out.println("서비스 상세보기 sNo: " + sNo);

		// 해당 서비스 정보 불러오기
		Service s = service.selectServiceView(sNo);

		DecimalFormat formatter = new DecimalFormat("###,###");
		s.setSPriceTxt(formatter.format(s.getSPrice()));

		System.out.println("메인카테고리 이름 : " + s.getMainCategoryName());
		System.out.println("서브카테고리 이름 : " + s.getSubCategoryName());

		if (s != null) {
			model.addAttribute("s", s);
		}
		int serviceNo = s.getSNo();

		// 브랜드 정보 불러오기
		String memberId = s.getMId();

		Member m = service.selectMemberName(memberId);
		model.addAttribute("m", m);

		// 서비스 파일 불러오기
		ArrayList<ServiceFile> fileList = service.fileList(sNo);
		System.out.println("fileList 사이즈 :" + fileList.size());
		System.out.println("fileList값:" + fileList.get(0));
		model.addAttribute("fileList", fileList);

		// 해당 유저가 등록한 다른서비스 불러오기
		ArrayList<Service> sList = service.userService(memberId);
		model.addAttribute("sList", sList);

		// 리뷰 리스트 불러오기 + 페이징
		ReviewPageData rpd = service.selectReviewList(sNo, reqPage,-1);
		if (rpd.getList().size() == 0) {
			System.out.println("리뷰 없음");
		} else {
			System.out.println("리뷰있음");
			System.out.println(rpd.getPageNavi());
		}

		model.addAttribute("reviewList", rpd.getList());
		model.addAttribute("pageNavi", rpd.getPageNavi());

		// 찜한내역 확인하기
		model.addAttribute("favoriteCheck", false);

		return "/service/serviceView";
	}

	// (문정) 결제 진행
	@RequestMapping("/insertServicePay.do")
	public String insertServicePay(ServicePay pay) {
		int result = service.insertServicePay(pay);
		if (result > 0) {
			result = service.updateTradeStatus(pay.getTNo());
		}
		return "redirect:/userTradeHistory.do?mNo=" + pay.getPNo();
	}

	// (도현)serviceList
	@RequestMapping("/serviceSearch.do")
	public String serviceSearch(@RequestParam(value = "page", defaultValue = "1") int page, String order, Model model,
			String keyword) {
		if (!(keyword == null || keyword.equals(""))) {
			// 네비 기능
			int listPerPage = 20;
			int maxListCount;
			maxListCount = service.selectServiceCount(keyword);

			List<Service> list = service.searchService(maxListCount - ((page) * listPerPage) + 1,
					maxListCount - ((page - 1) * listPerPage), keyword);
			int maxPrintPageCount = 5;
			int maxPageCount = service.selectMaxPageCount(listPerPage, maxListCount);
			int begin = maxPrintPageCount * (page / (maxPrintPageCount + 1)) + 1; // 네비 시작
			int end = (begin + 4) < maxPageCount ? begin + 4 : maxPageCount; // 네비 끝

			// 천단위 컴마 찍기
			DecimalFormat formatter = new DecimalFormat("###,###");
			for (int i = 0; i < list.size(); i++) {
				list.get(i).setSPriceTxt(formatter.format(list.get(i).getSPrice()));
			}
			model.addAttribute("list", list);
			model.addAttribute("begin", begin);
			model.addAttribute("end", end);
			model.addAttribute("maxListCount", formatter.format(maxListCount));
			model.addAttribute("maxPageCount", maxPageCount);
		}
		return "/service/serviceAllList";
	}

	@ResponseBody
	@RequestMapping
	public int isPossibleMakeService(String mId) {
		int count = service.selectFreeServiceCount(mId);
		return count;
	}
}
