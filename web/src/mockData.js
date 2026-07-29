export const mockUser = {
  id: "u_101",
  name: "Tanvir Hasan",
  flatNo: "4-A",
  buildingBlock: "Building Alpha (A-Block)",
  phone: "+880 1711-234567",
  email: "tanvir.hasan@kunjachaya.org",
  role: "MEMBER", // "MEMBER" or "ADMIN"
  familyMembers: 4,
  vehicles: "Car: Dhaka Metro Ga-42-1029",
  joinedDate: "2023-01-15"
};

export const mockAdminUser = {
  id: "u_admin",
  name: "Engr. Rafiqul Islam",
  flatNo: "6-B",
  buildingBlock: "Building Executive (A-Block)",
  phone: "+880 1819-987654",
  email: "president@kunjachaya.org",
  role: "ADMIN",
  familyMembers: 3,
  vehicles: "Car: Dhaka Metro Ga-11-9002",
  joinedDate: "2020-05-10"
};

export const mockFinancials = [
  {
    id: "FIN-2026-07",
    titleEn: "Monthly Maintenance & Security Dues - July 2026",
    titleBn: "মাসিক রক্ষণাবেক্ষণ ও সিকিউরিটি ফি - জুলাই ২০২৬",
    month: "July 2026",
    amount: 3500,
    dueDate: "2026-07-31",
    status: "UNPAID", // UNPAID, PAID, OVERDUE
    category: "Maintenance",
    paymentDate: null,
    paymentMethod: null,
    txRef: null
  },
  {
    id: "FIN-2026-06",
    titleEn: "Monthly Maintenance & Security Dues - June 2026",
    titleBn: "মাসিক রক্ষণাবেক্ষণ ও সিকিউরিটি ফি - জুন ২০২৬",
    month: "June 2026",
    amount: 3500,
    dueDate: "2026-06-30",
    status: "PAID",
    category: "Maintenance",
    paymentDate: "2026-06-25",
    paymentMethod: "bKash Mobile Wallet",
    txRef: "BKS-991204821"
  },
  {
    id: "FIN-2026-SPORTS",
    titleEn: "Annual Sports & Cultural Festival Fund 2026",
    titleBn: "বার্ষিক ক্রীড়া ও সাংস্কৃতিক উৎসব তহবিল ২০২৬",
    month: "June 2026",
    amount: 1500,
    dueDate: "2026-06-15",
    status: "PAID",
    category: "Event Fund",
    paymentDate: "2026-06-10",
    paymentMethod: "Nagad Wallet",
    txRef: "NGD-88201923"
  },
  {
    id: "FIN-2026-ELEVATOR",
    titleEn: "Elevator Modernization Special Levy",
    titleBn: "লিফট আধুনিকায়ন বিশেষ অনুদান তহিবল",
    month: "May 2026",
    amount: 5000,
    dueDate: "2026-05-31",
    status: "PAID",
    category: "Capital Work",
    paymentDate: "2026-05-28",
    paymentMethod: "Bank Transfer",
    txRef: "CBL-00128941"
  }
];

export const mockNotices = [
  {
    id: "NTC-001",
    titleEn: "Tree Plantation Drive & Resident Meet 2026",
    titleBn: "বৃক্ষরোপণ অভিযান ও আবাসিক মিলনমেলা ২০২৬",
    category: "Urgent",
    date: "2026-07-25",
    isPinned: true,
    contentEn: "All members are warmly invited to join the greening initiative at Kunjachaya Central Park this Friday at 9:00 AM. Refreshments will be served.",
    contentBn: "আগামী শুক্রবার সকাল ৯:০০ টায় কুঞ্জছায়া সেন্ট্রাল পার্কে সবুজায়ন উদ্যোগে সকল সদস্যকে সাদর আমন্ত্রণ জানানো হচ্ছে। হালকা নাস্তার ব্যবস্থা রয়েছে।"
  },
  {
    id: "NTC-002",
    titleEn: "Rooftop Solar Panel Cleaning & Inverter Maintenance Schedule",
    titleBn: "ছাদের সোলার প্যানেল পরিষ্কার ও ইনভার্টার রক্ষণাবেক্ষণ সূচি",
    category: "Maintenance",
    date: "2026-07-20",
    isPinned: false,
    contentEn: "Power backup inverter testing will take place between 2:00 PM to 4:00 PM on Sunday. Solar power output optimization drive in progress.",
    contentBn: "আগামী রবিবার দুপুর ২:০০ থেকে ৪:০০ পর্যন্ত সোলার পাওয়ার ইনভার্টার সার্ভিসিং অনুষ্ঠিত হবে।"
  },
  {
    id: "NTC-003",
    titleEn: "Water Tank Deep Cleaning & Sanitation Maintenance Notice",
    titleBn: "আন্ডারগ্রাউন্ড ও ওভারহেড পানির ট্যাংক জীবাণুমুক্তকরণ",
    category: "General",
    date: "2026-07-15",
    isPinned: false,
    contentEn: "Water supply will be temporarily paused from 8:00 AM to 12:00 PM on Monday for annual chlorination and high-pressure washing.",
    contentBn: "বার্ষিক পানির ট্যাংক পরিষ্কার ও জীবাণুমুক্তকরণের জন্য সোমবার সকাল ৮:০০ থেকে দুপুর ১২:০০ পর্যন্ত পানি সরবরাহ বন্ধ থাকবে।"
  }
];

export const mockComplaints = [
  {
    id: "CMP-1082",
    flatNo: "4-A",
    titleEn: "Kitchen Sink Pipe Seepage & Drainage Blockage",
    titleBn: "রান্নাঘরের পাইপে লিকেজ ও ড্রেনেজ সমস্যা",
    category: "Plumbing",
    status: "IN_PROGRESS",
    priority: "HIGH",
    date: "2026-07-24",
    descriptionEn: "Low water pressure in main tap and slow drainage in the kitchen sink since yesterday evening.",
    adminResponseEn: "Plumber assigned. Inspection scheduled today at 4:30 PM."
  },
  {
    id: "CMP-1045",
    flatNo: "4-A",
    titleEn: "Corridor Light Bulb Replacement Near Elevator A2",
    titleBn: "লিফট এ২ এর সামনের করিডোরের লাইট পরিবর্তন",
    category: "Electrical",
    status: "RESOLVED",
    priority: "MEDIUM",
    date: "2026-07-10",
    descriptionEn: "4th floor corridor LED light is flickering frequently.",
    adminResponseEn: "Replaced with new Phillips LED bulb on July 11."
  }
];

export const mockEvents = [
  {
    id: "EVT-01",
    titleEn: "Annual Inter-Flat Chess & Table Tennis Tournament 2026",
    titleBn: "বার্ষিক আন্তঃফ্ল্যাট দাবা ও টেবিল টেনিস প্রতিযোগিতা ২০২৬",
    date: "2026-08-05",
    time: "10:00 AM - 6:00 PM",
    venueEn: "Kunjachaya Club Indoor Community Sports Lounge",
    venueBn: "কুঞ্জছায়া ক্লাব ইনডোর স্পোর্টস লাউঞ্জ",
    category: "Sports",
    entryFee: 0,
    attendingCount: 28,
    isRegistered: true
  },
  {
    id: "EVT-02",
    titleEn: "Grand Independence Day Cultural Evening & Dinner",
    titleBn: "স্বাধীনতার স্বর্ণজয়ন্তী সাংস্কৃতিক সন্ধ্যা ও নাইট ডিনার",
    date: "2026-08-15",
    time: "6:30 PM Onwards",
    venueEn: "Kunjachaya Central Green Lawn",
    venueBn: "কুঞ্জছায়া সেন্ট্রাল গ্রীন লন",
    category: "Cultural",
    entryFee: 0,
    attendingCount: 64,
    isRegistered: false
  }
];

export const mockMembers = [
  { flatNo: "1-A", name: "Dr. Anisur Rahman", phone: "+880 1711-100200", profession: "Consultant Physician", status: "Active" },
  { flatNo: "1-B", name: "Mahmudul Hasan", phone: "+880 1819-300400", profession: "Software Architect", status: "Active" },
  { flatNo: "2-A", name: "Shamsul Alam", phone: "+880 1912-500600", profession: "Retired Banker", status: "Active" },
  { flatNo: "2-B", name: "Nusrat Jahan", phone: "+880 1611-700800", profession: "University Associate Professor", status: "Active" },
  { flatNo: "3-A", name: "Kabir Hossain", phone: "+880 1713-900100", profession: "Civil Engineer", status: "Active" },
  { flatNo: "3-B", name: "Tariqul Islam", phone: "+880 1811-222333", profession: "Business Owner", status: "Active" },
  { flatNo: "4-A", name: "Tanvir Hasan", phone: "+880 1711-234567", profession: "Finance Manager", status: "Active" },
  { flatNo: "4-B", name: "Farhana Parveen", phone: "+880 1911-444555", profession: "Architect", status: "Active" },
  { flatNo: "5-A", name: "Adv. Shahadat Hossain", phone: "+880 1715-666777", profession: "Supreme Court Advocate", status: "Active" },
  { flatNo: "5-B", name: "Kamrul Ahsan", phone: "+880 1818-888999", profession: "Senior Executive", status: "Active" }
];

export const mockEmergencyContacts = [
  { role: "Main Security Guard Room", name: "Guard Master Control", phone: "+880 1700-000101", icon: "Shield" },
  { role: "Building Senior Caretaker", name: "Abdul Jalil", phone: "+880 1700-000102", icon: "UserCheck" },
  { role: "On-Call Electrician & Generator Tech", name: "Sumon Miah", phone: "+880 1700-000103", icon: "Zap" },
  { role: "On-Call Plumber & Water Pump Technician", name: "Basher Hossain", phone: "+880 1700-000104", icon: "Wrench" },
  { role: "Nearest Police Station (Gulshan/Dhanmondi)", name: "Duty Officer", phone: "999 / 02-9881023", icon: "Siren" },
  { role: "Nearest Fire Brigade Control Room", name: "Fire Service Station", phone: "16163 / 02-9555555", icon: "Flame" }
];
